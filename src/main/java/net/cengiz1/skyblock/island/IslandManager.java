package net.cengiz1.skyblock.island;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.config.SettingsManager;
import net.cengiz1.skyblock.proxy.ProxyManager;
import net.cengiz1.skyblock.schematic.FaweSchematicService;
import net.cengiz1.skyblock.schematic.SchematicService;
import net.cengiz1.skyblock.storage.Storage;
import net.cengiz1.skyblock.upgrade.UpgradeManager;
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

    // Boyut yükseltmesi için; SkyblockPlugin tarafından sonradan bağlanır.
    private UpgradeManager upgradeManager;

    // Proxy modülü; etkinse ProxyManager.start() tarafından bağlanır. null = proxy kapalı.
    private ProxyManager proxyManager;
    // Bu backend sunucunun adı (proxy açıkken). null = proxy kapalı, tüm adalar yerel.
    private String localServerName;

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

    public void setUpgradeManager(UpgradeManager upgradeManager) {
        this.upgradeManager = upgradeManager;
    }

    public void setProxyManager(ProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    public void setLocalServerName(String localServerName) {
        this.localServerName = localServerName;
    }

    public Collection<Island> getAllIslands() {
        return this.islandsById.values();
    }

    /**
     * Tek bir adayı veritabanından yeniden yükleyip önbelleği tazeler (proxy senkronu).
     * DB'de yoksa önbellekten kaldırır. Async thread'den çağrılabilir (eşzamanlı map'ler).
     */
    public void reloadIsland(UUID islandId) {
        Island fresh = this.storage.load(islandId);
        if (fresh == null) {
            removeFromCache(islandId);
            return;
        }
        Island existing = this.islandsById.get(islandId);
        if (existing != null && !existing.getOwner().equals(fresh.getOwner()))
            this.ownerToIsland.remove(existing.getOwner());
        this.islandsById.put(islandId, fresh);
        this.ownerToIsland.put(fresh.getOwner(), islandId);
    }

    /** Adayı yalnızca yerel önbellekten kaldırır (DB'ye dokunmaz). */
    public void removeFromCache(UUID islandId) {
        Island existing = this.islandsById.remove(islandId);
        if (existing != null)
            this.ownerToIsland.remove(existing.getOwner());
    }

    /** Adanın etkin koruma yarıçapı (boyut yükseltmesine göre). */
    public int getProtectionHalf(Island island) {
        double size = this.upgradeManager != null
                ? this.upgradeManager.getValue(island, "size", settings.getIslandSize())
                : settings.getIslandSize();
        return (int) Math.max(1, size / 2);
    }

    public Island getIslandAt(Location location) {
        if (location.getWorld() == null)
            return null;

        String worldName = location.getWorld().getName();

        for (Island island : this.islandsById.values()) {
            if (!island.getWorldName().equals(worldName))
                continue;
            // Proxy açıkken: başka sunucuda barınan adaları yok say (fiziksel olarak burada değiller).
            if (this.localServerName != null && island.getServerName() != null
                    && !island.getServerName().equals(this.localServerName))
                continue;
            int half = getProtectionHalf(island);
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

    /** Bir oyuncunun üyesi olduğu adayı döner (sahip veya üye). */
    public Island getByMember(UUID playerId) {
        Island own = getByOwner(playerId);
        if (own != null)
            return own;
        for (Island island : this.islandsById.values())
            if (island.isMember(playerId))
                return island;
        return null;
    }

    /** Ada sahipliğini devreder. Eski sahip moderatör üye olur. */
    public void transferOwnership(Island island, UUID newOwner) {
        UUID oldOwner = island.getOwner();
        this.ownerToIsland.remove(oldOwner);
        island.removeMember(newOwner);
        island.setOwner(newOwner);
        island.setRole(oldOwner, IslandRole.MODERATOR);
        this.ownerToIsland.put(newOwner, island.getUniqueId());
        saveAsync(island);
    }

    public void deleteIsland(Island island) {
        this.islandsById.remove(island.getUniqueId());
        this.ownerToIsland.remove(island.getOwner());

        World world = this.worldManager.getWorld();

        // Adada bulunan çevrimiçi üyeleri önce güvenli yere ışınla (boşluğa düşmesinler).
        Location safe = Bukkit.getWorlds().get(0).getSpawnLocation();
        for (UUID id : island.getAllMemberIds()) {
            Player member = Bukkit.getPlayer(id);
            if (member != null && member.isOnline() && getIslandAt(member.getLocation()) == island)
                member.teleport(safe);
        }

        // Tüm ada bölgesini (grid hücresini) temizle; sadece merkez platformu değil.
        int half = Math.max(2, settings.getIslandDistance() / 2 - 1);
        int centerX = island.getCenterX();
        int centerY = island.getCenterY();
        int centerZ = island.getCenterZ();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // FAWE bölge temizliği (async destekler); başarısızsa ana thread'de yedek platform temizliği.
            boolean cleared = this.schematicService.clearRegion(world, centerX, centerZ, half);
            if (!cleared)
                Bukkit.getScheduler().runTask(plugin, () -> clearPlatform(world, centerX, centerY, centerZ));

            this.storage.delete(island.getUniqueId());
            if (this.proxyManager != null && this.proxyManager.isEnabled())
                this.proxyManager.publishIslandDelete(island.getUniqueId());
        });
    }

    public void teleportHome(Player player, Island island) {
        World world = this.worldManager.getWorld();
        Location home = island.getHome(world);
        world.getChunkAt(home);
        player.teleport(home);
    }

    /** Adanın çevrimiçi tüm üyelerine (sahip dahil) mesaj gönderir. */
    public void messageMembers(Island island, String messageKey, String... replacements) {
        for (UUID id : island.getAllMemberIds()) {
            Player member = Bukkit.getPlayer(id);
            if (member != null && member.isOnline())
                plugin.getMessages().send(member, messageKey, replacements);
        }
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            this.storage.save(island);
            if (this.proxyManager != null && this.proxyManager.isEnabled())
                this.proxyManager.queueIslandSync(island.getUniqueId());
        });
    }

    public void shutdown() {
        this.creationService.shutdown();
        saveAll();
    }
}
