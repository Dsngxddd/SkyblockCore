package net.cengiz1.skyblock.island;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.config.SettingsManager;
import net.cengiz1.skyblock.schematic.FaweSchematicService;
import net.cengiz1.skyblock.schematic.SchematicService;
import net.cengiz1.skyblock.storage.Storage;
import net.cengiz1.skyblock.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandManager {

    private final SkyblockPlugin plugin;
    private final SettingsManager settings;
    private final Storage storage;
    private final WorldManager worldManager;
    private final IslandGrid grid;
    private final SchematicService schematicService;
    private final IslandCreationService creationService;

    private final Map<UUID, Island> islandsById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> ownerToIsland = new ConcurrentHashMap<>();

    public IslandManager(SkyblockPlugin plugin, SettingsManager settings, Storage storage, WorldManager worldManager) {
        this.plugin = plugin;
        this.settings = settings;
        this.storage = storage;
        this.worldManager = worldManager;
        this.grid = new IslandGrid(settings);
        this.schematicService = new FaweSchematicService(plugin);
        this.creationService = new IslandCreationService(plugin, settings, storage, worldManager, this, this.schematicService);
    }

    public void loadAll() {
        int highestIndex = -1;
        for (Island island : this.storage.loadAll()) {
            this.islandsById.put(island.getUniqueId(), island);
            this.ownerToIsland.put(island.getOwner(), island.getUniqueId());
            if (island.getGridIndex() > highestIndex)
                highestIndex = island.getGridIndex();
        }
        this.grid.setNextIndex(highestIndex + 1);
        plugin.getLogger().info("Loaded " + this.islandsById.size() + " islands.");
    }

    public IslandGrid getGrid() {
        return grid;
    }

    public IslandCreationService getCreationService() {
        return creationService;
    }

    public SchematicService getSchematicService() {
        return schematicService;
    }

    public Island getByOwner(UUID owner) {
        UUID islandId = this.ownerToIsland.get(owner);
        return islandId == null ? null : this.islandsById.get(islandId);
    }

    public Island getById(UUID islandId) {
        return this.islandsById.get(islandId);
    }

    public Island getIslandAt(Location location) {
        if (location.getWorld() == null)
            return null;

        String worldName = location.getWorld().getName();
        int half = settings.getIslandSize() / 2;

        for (Island island : this.islandsById.values()) {
            if (!island.getWorldName().equals(worldName))
                continue;
            if (Math.abs(location.getBlockX() - island.getCenterX()) <= half &&
                    Math.abs(location.getBlockZ() - island.getCenterZ()) <= half)
                return island;
        }

        return null;
    }

    public void register(Island island) {
        this.islandsById.put(island.getUniqueId(), island);
        this.ownerToIsland.put(island.getOwner(), island.getUniqueId());
    }

    public void deleteIsland(Island island) {
        this.islandsById.remove(island.getUniqueId());
        this.ownerToIsland.remove(island.getOwner());

        World world = this.worldManager.getWorld();
        clearPlatform(world, island.getCenterX(), island.getCenterY(), island.getCenterZ());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> this.storage.delete(island.getUniqueId()));
    }

    public void teleportHome(Player player, Island island) {
        World world = this.worldManager.getWorld();
        Location home = island.getHome(world);
        world.getChunkAt(home);
        player.teleport(home);
    }

    void buildDefaultPlatform(World world, int centerX, int centerY, int centerZ) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Block block = world.getBlockAt(centerX + dx, centerY, centerZ + dz);
                block.setType(Material.GRASS_BLOCK, false);
            }
        }
        world.getBlockAt(centerX, centerY - 1, centerZ).setType(Material.BEDROCK, false);
    }

    private void clearPlatform(World world, int centerX, int centerY, int centerZ) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(centerX + dx, centerY, centerZ + dz).setType(Material.AIR, false);
            }
        }
        world.getBlockAt(centerX, centerY - 1, centerZ).setType(Material.AIR, false);
    }

    public void saveAll() {
        Collection<Island> islands = this.islandsById.values();
        for (Island island : islands)
            this.storage.save(island);
    }

    public void saveAsync(Island island) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> this.storage.save(island));
    }

    public void shutdown() {
        this.creationService.shutdown();
        saveAll();
    }
}
