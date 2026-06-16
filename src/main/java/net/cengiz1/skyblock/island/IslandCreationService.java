package net.cengiz1.skyblock.island;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.config.SettingsManager;
import net.cengiz1.skyblock.proxy.ProxyManager;
import net.cengiz1.skyblock.schematic.SchematicDefinition;
import net.cengiz1.skyblock.schematic.SchematicService;
import net.cengiz1.skyblock.storage.Storage;
import net.cengiz1.skyblock.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IslandCreationService {

    public enum Result {
        SUCCESS,
        ALREADY_HAS_ISLAND,
        ALREADY_CREATING,
        INVALID_SCHEMATIC,
        FAILED
    }

    private final SkyblockPlugin plugin;
    private final SettingsManager settings;
    private final Storage storage;
    private final WorldManager worldManager;
    private final IslandManager islandManager;
    private final SchematicService schematicService;

    private final ExecutorService executor;
    private final Semaphore concurrencyLimit;
    private final Set<UUID> creating = ConcurrentHashMap.newKeySet();

    public IslandCreationService(SkyblockPlugin plugin, SettingsManager settings, Storage storage,
                                 WorldManager worldManager, IslandManager islandManager,
                                 SchematicService schematicService) {
        this.plugin = plugin;
        this.settings = settings;
        this.storage = storage;
        this.worldManager = worldManager;
        this.islandManager = islandManager;
        this.schematicService = schematicService;
        this.executor = createExecutor(settings.getCreationThreads());
        this.concurrencyLimit = new Semaphore(settings.getMaxConcurrentCreations());
    }

    public CompletableFuture<Result> create(Player player) {
        return create(player, null);
    }

    public CompletableFuture<Result> create(Player player, String schematicKey) {
        UUID playerId = player.getUniqueId();

        if (islandManager.getByOwner(playerId) != null)
            return CompletableFuture.completedFuture(Result.ALREADY_HAS_ISLAND);

        String resolvedKey = schematicKey != null ? schematicKey : schematicService.getDefaultKey();
        boolean useSchematic = schematicService.isReady() && resolvedKey != null && schematicService.has(resolvedKey);

        if (schematicKey != null && schematicService.isReady() && !schematicService.has(schematicKey))
            return CompletableFuture.completedFuture(Result.INVALID_SCHEMATIC);

        if (!this.creating.add(playerId))
            return CompletableFuture.completedFuture(Result.ALREADY_CREATING);

        boolean natural = !this.settings.isVoidWorld();
        SchematicDefinition definition = (useSchematic && !natural) ? schematicService.get(resolvedKey) : null;

        CompletableFuture<Result> result = new CompletableFuture<>();
        AtomicBoolean acquired = new AtomicBoolean(false);

        this.executor.submit(() -> {
            try {
                this.concurrencyLimit.acquire();
                acquired.set(true);

                if (islandManager.getByOwner(playerId) != null) {
                    result.complete(Result.ALREADY_HAS_ISLAND);
                    return;
                }

                World world = this.worldManager.getWorld();
                int index = islandManager.getGrid().reserveIndex();
                int centerX = islandManager.getGrid().getCenterX(index);
                int centerY = this.settings.getIslandHeight();
                int centerZ = islandManager.getGrid().getCenterZ(index);

                double offsetX = definition != null ? definition.getHomeOffsetX() : 0.5;
                double offsetY = definition != null ? definition.getHomeOffsetY() : 1.0;
                double offsetZ = definition != null ? definition.getHomeOffsetZ() : 0.5;

                Island island = new Island(UUID.randomUUID(), playerId, world.getName(),
                        index, centerX, centerY, centerZ);
                island.setHome(centerX + offsetX, centerY + offsetY, centerZ + offsetZ, 0f, 0f);

                ProxyManager proxy = plugin.getProxyManager();
                if (proxy != null && proxy.isEnabled())
                    island.setServerNameRaw(proxy.getServerName());

                this.storage.save(island);

                if (proxy != null && proxy.isEnabled())
                    proxy.publishIslandUpdate(island.getUniqueId());

                boolean pasted = false;
                if (definition != null) {
                    try {
                        this.schematicService.paste(definition, world, centerX, centerY, centerZ);
                        pasted = true;
                    } catch (Throwable error) {
                        plugin.getLogger().warning("Schematic paste failed (" + definition.getKey()
                                + "), using fallback platform: " + error.getMessage());
                        island.setHome(centerX + 0.5, centerY + 1, centerZ + 0.5, 0f, 0f);
                        this.storage.save(island);
                    }
                }

                boolean fallback = !pasted;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        if (natural) {
                            world.getChunkAt(centerX >> 4, centerZ >> 4).load(true);
                            org.bukkit.block.Block top = world.getHighestBlockAt(centerX, centerZ);
                            int surfaceY;
                            if (top.isLiquid() || top.getY() < world.getSeaLevel()) {
                                surfaceY = world.getSeaLevel();
                                islandManager.buildDefaultPlatform(world, centerX, surfaceY, centerZ);
                            } else {
                                surfaceY = top.getY();
                            }
                            island.setHome(centerX + 0.5, surfaceY + 1, centerZ + 0.5, 0f, 0f);
                        } else if (fallback) {
                            islandManager.buildDefaultPlatform(world, centerX, centerY, centerZ);
                        }
                        islandManager.register(island);

                        Player online = Bukkit.getPlayer(playerId);
                        if (online != null && online.isOnline()) {
                            Location home = island.getHome(world);
                            world.getChunkAt(home);
                            online.teleport(home);

                            if (islandManager.getBorderManager() != null)
                                islandManager.getBorderManager().apply(online, island);
                        }

                        Bukkit.getPluginManager().callEvent(
                                new net.cengiz1.skyblock.event.IslandCreateEvent(island));
                        result.complete(Result.SUCCESS);
                    } catch (Throwable error) {
                        plugin.getLogger().warning("Island finalize failed for " + playerId + ": " + error.getMessage());
                        result.complete(Result.FAILED);
                    }
                });
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                result.complete(Result.FAILED);
            } catch (Throwable error) {
                plugin.getLogger().warning("Island creation failed for " + playerId + ": " + error.getMessage());
                result.complete(Result.FAILED);
            }
        });

        return result.whenComplete((value, error) -> {
            if (acquired.getAndSet(false))
                this.concurrencyLimit.release();
            this.creating.remove(playerId);
        });
    }

    public boolean isCreating(UUID playerId) {
        return this.creating.contains(playerId);
    }

    public void shutdown() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(10, TimeUnit.SECONDS))
                this.executor.shutdownNow();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            this.executor.shutdownNow();
        }
    }

    private ExecutorService createExecutor(int threads) {
        AtomicInteger counter = new AtomicInteger();
        return Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "skyblock-creation-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }
}
