package net.cengiz1.skyblock.proxy;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.config.SettingsManager;
import net.cengiz1.skyblock.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxy modülünün beyni. Redis pub/sub ile sunucular arası ada senkronu sağlar,
 * oyuncuyu adasının barındığı sunucuya yönlendirir ve bekleyen ışınlanmaları tamamlar.
 *
 * Mimari: tüm skyblock backend'leri aynı MySQL'i paylaşır. Her ada fiziksel olarak
 * tek bir sunucuda barınır (Island.serverName). Veri değişimleri Redis üzerinden
 * yayınlanır; diğer sunucular ilgili adayı DB'den tazeler.
 */
public class ProxyManager {

    private final SkyblockPlugin plugin;

    private boolean enabled;
    private String serverName;
    private String spawnServer;
    private String channel;
    private int pendingTtlSeconds;

    private RedisMessenger messenger;
    private ServerConnector connector;

    // Yüksek frekanslı kayıtları (blok/level) biriktirip toplu yayınlamak için.
    private final Set<UUID> pendingSync = ConcurrentHashMap.newKeySet();
    private BukkitTask syncFlushTask;

    public ProxyManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getServerName() {
        return serverName;
    }

    public String getSpawnServer() {
        return spawnServer;
    }

    /**
     * Proxy modülünü başlatır. Redis'e bağlanamazsa modül kapalı kalır;
     * ana skyblock plugini her durumda çalışmaya devam eder.
     */
    public void start() {
        SettingsManager settings = plugin.getSettings();
        if (!settings.isProxyEnabled()) {
            this.enabled = false;
            return;
        }

        this.serverName = settings.getProxyServerName();
        this.spawnServer = settings.getProxySpawnServer();
        this.channel = settings.getProxyRedisChannel();
        this.pendingTtlSeconds = settings.getProxyPendingSeconds();

        if (serverName == null || serverName.isEmpty()) {
            plugin.getLogger().severe("Proxy: 'proxy.server-name' boş bırakılamaz; proxy modülü kapatıldı.");
            this.enabled = false;
            return;
        }

        try {
            this.messenger = new RedisMessenger(plugin.getLogger(),
                    settings.getProxyRedisHost(), settings.getProxyRedisPort(),
                    settings.getProxyRedisUsername(), settings.getProxyRedisPassword(),
                    settings.getProxyRedisTimeout(), channel, this::onMessage);
        } catch (Throwable error) {
            plugin.getLogger().severe("Proxy: Redis'e bağlanılamadı, proxy modülü kapatıldı: " + error.getMessage());
            this.enabled = false;
            return;
        }

        this.connector = new ServerConnector(plugin);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, ServerConnector.BUNGEE_CHANNEL);

        this.enabled = true;
        plugin.getIslandManager().setLocalServerName(serverName);
        plugin.getIslandManager().setProxyManager(this);

        // Birikmiş senkron işaretlerini periyodik (5 sn) yayınla; blok/level selini önler.
        this.syncFlushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushSync, 100L, 100L);

        plugin.getLogger().info("Proxy modülü etkin. Bu sunucunun adı: " + serverName);
    }

    public void stop() {
        if (syncFlushTask != null)
            syncFlushTask.cancel();
        if (enabled)
            flushSync();
        if (messenger != null)
            messenger.shutdown();
        this.enabled = false;
    }

    /** Ada bu sunucuda mı barınıyor? (Atanmamış/eski adalar yerel sayılır.) */
    public boolean isLocal(Island island) {
        if (!enabled)
            return true;
        String host = island.getServerName();
        return host == null || host.equals(serverName);
    }

    /**
     * Adaya ışınlama isteğini ele alır. Ada başka sunucudaysa oyuncuyu oraya yönlendirir,
     * bekleyen ışınlanmayı kaydeder ve true döner. Yerelse false döner (çağıran yerel ışınlar).
     */
    public boolean handleTeleport(Player player, Island island) {
        if (!enabled)
            return false;

        String target = island.getServerName();
        if (target == null) {
            // Migrasyon: sunucusu atanmamış eski adayı bu sunucuya stampla, yerel ışınla.
            island.setServerName(serverName);
            plugin.getIslandManager().saveAsync(island);
            return false;
        }
        if (target.equals(serverName))
            return false;

        // Hedef sunucuya bağlanınca tamamlanacak ışınlanmayı Redis'e yaz.
        messenger.setWithExpiry(pendingKey(player.getUniqueId()), island.getUniqueId().toString(), pendingTtlSeconds);
        plugin.getMessages().send(player, "proxy-sending", "{server}", target);
        connector.connect(player, target);
        return true;
    }

    /** Oyuncu giriş yaptığında bekleyen (çapraz sunucu) ışınlanmayı tamamlamayı dener. */
    public void tryCompletePendingTeleport(Player player) {
        if (!enabled)
            return;
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String value = messenger.takeValue(pendingKey(playerId));
            if (value == null)
                return;
            UUID islandId;
            try {
                islandId = UUID.fromString(value);
            } catch (IllegalArgumentException error) {
                return;
            }
            // Cache'te yoksa DB'den yükle (ör. bu sunucu açıldıktan sonra oluşmuş ada).
            if (plugin.getIslandManager().getById(islandId) == null)
                plugin.getIslandManager().reloadIsland(islandId);

            // Dünya/parça yüklensin diye kısa gecikmeyle ana thread'de ışınla.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player online = Bukkit.getPlayer(playerId);
                Island island = plugin.getIslandManager().getById(islandId);
                if (online != null && online.isOnline() && island != null) {
                    plugin.getMessages().send(online, "teleporting");
                    plugin.getIslandManager().teleportHome(online, island);
                }
            }, 20L);
        });
    }

    // ───────────── Senkron yayınları ─────────────

    /** Yüksek frekanslı kayıtlar için: adayı toplu yayın kuyruğuna alır. */
    public void queueIslandSync(UUID islandId) {
        if (enabled)
            pendingSync.add(islandId);
    }

    public void publishIslandUpdate(UUID islandId) {
        if (enabled)
            messenger.publish(ProxyMessage.island(MessageType.ISLAND_UPDATE, serverName, islandId).serialize());
    }

    public void publishIslandDelete(UUID islandId) {
        if (enabled)
            messenger.publish(ProxyMessage.island(MessageType.ISLAND_DELETE, serverName, islandId).serialize());
    }

    private void flushSync() {
        if (!enabled || pendingSync.isEmpty())
            return;
        for (UUID islandId : new ArrayList<>(pendingSync)) {
            pendingSync.remove(islandId);
            messenger.publish(ProxyMessage.island(MessageType.ISLAND_UPDATE, serverName, islandId).serialize());
        }
    }

    // ───────────── Gelen mesajlar ─────────────

    private void onMessage(String raw) {
        ProxyMessage message = ProxyMessage.parse(raw);
        if (message == null)
            return;
        if (serverName.equals(message.getOrigin()))
            return; // kendi yayınımız
        UUID islandId = message.getIslandId();
        if (islandId == null)
            return;

        switch (message.getType()) {
            case ISLAND_UPDATE:
                runAsyncSafe(() -> plugin.getIslandManager().reloadIsland(islandId));
                break;
            case ISLAND_DELETE:
                plugin.getIslandManager().removeFromCache(islandId);
                break;
            default:
                break;
        }
    }

    private void runAsyncSafe(Runnable task) {
        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } catch (IllegalStateException disabling) {
            // Plugin kapanıyor; görmezden gel.
        }
    }

    private String pendingKey(UUID playerId) {
        return channel + ":pending:" + playerId;
    }
}
