package net.cengiz1.skyblock.config;

import net.cengiz1.skyblock.SkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettingsManager {

    private final SkyblockPlugin plugin;

    private String worldName;
    private int islandHeight;
    private int islandDistance;
    private int islandSize;
    private boolean borderEnabled;
    private String borderColor;

    private String spawnWorld;
    private double spawnX;
    private double spawnY;
    private double spawnZ;
    private float spawnYaw;
    private float spawnPitch;

    private int maxConcurrentCreations;
    private int creationThreads;

    private String commandName;
    private List<String> commandAliases = new ArrayList<>();
    private Map<String, List<String>> subcommandAliases = new LinkedHashMap<>();
    private int inviteExpireSeconds;
    private boolean economyEnabled;

    private String storageType;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private boolean useSsl;

    private boolean proxyEnabled;
    private boolean proxyDebug;
    private String proxyServerName;
    private String proxySpawnServer;
    private List<String> proxyCreateServers = new ArrayList<>();
    private int proxyPendingSeconds;
    private String proxyRedisHost;
    private int proxyRedisPort;
    private String proxyRedisUsername;
    private String proxyRedisPassword;
    private int proxyRedisTimeout;
    private String proxyRedisChannel;

    public SettingsManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();

        this.worldName = config.getString("world.name", "islands");
        this.islandHeight = config.getInt("world.island-height", 100);
        this.islandDistance = config.getInt("world.island-distance", 250);
        this.islandSize = config.getInt("world.island-size", 100);
        this.borderEnabled = config.getBoolean("border.enabled", true);
        this.borderColor = config.getString("border.color", "BLUE").toUpperCase();

        this.spawnWorld = config.getString("spawn.world", "");
        this.spawnX = config.getDouble("spawn.x", 0.5);
        this.spawnY = config.getDouble("spawn.y", 100);
        this.spawnZ = config.getDouble("spawn.z", 0.5);
        this.spawnYaw = (float) config.getDouble("spawn.yaw", 0);
        this.spawnPitch = (float) config.getDouble("spawn.pitch", 0);

        this.maxConcurrentCreations = Math.max(1, config.getInt("creation.max-concurrent", 3));
        this.creationThreads = Math.max(1, config.getInt("creation.threads", 3));

        this.commandName = config.getString("command.name", "ada").toLowerCase();
        this.commandAliases = config.getStringList("command.aliases");
        this.inviteExpireSeconds = Math.max(10, config.getInt("command.invite-expire-seconds", 120));
        this.economyEnabled = config.getBoolean("economy.enabled", true);

        this.subcommandAliases = new LinkedHashMap<>();
        ConfigurationSection subs = config.getConfigurationSection("command.subcommands");
        if (subs != null) {
            for (String key : subs.getKeys(false)) {
                List<String> aliases = subs.getStringList(key);
                if (aliases.isEmpty())
                    aliases.add(key);
                this.subcommandAliases.put(key.toLowerCase(), aliases);
            }
        }

        this.storageType = config.getString("storage.type", "sqlite").toLowerCase();
        this.host = config.getString("storage.mysql.host", "localhost");
        this.port = config.getInt("storage.mysql.port", 3306);
        this.database = config.getString("storage.mysql.database", "skyblock");
        this.username = config.getString("storage.mysql.username", "root");
        this.password = config.getString("storage.mysql.password", "");
        this.useSsl = config.getBoolean("storage.mysql.ssl", false);

        this.proxyEnabled = config.getBoolean("proxy.enabled", false);
        this.proxyDebug = config.getBoolean("proxy.debug", false);
        this.proxyServerName = config.getString("proxy.server-name", "skyblock-1");
        this.proxySpawnServer = config.getString("proxy.spawn-server", "spawn-1");

        this.proxyCreateServers = new ArrayList<>(config.getStringList("proxy.create-servers"));
        if (this.proxyCreateServers.isEmpty()) {
            String single = config.getString("proxy.create-server", "");
            if (single != null && !single.isEmpty())
                this.proxyCreateServers.add(single);
        }
        this.proxyPendingSeconds = Math.max(5, config.getInt("proxy.pending-teleport-seconds", 30));
        this.proxyRedisHost = config.getString("proxy.redis.host", "localhost");
        this.proxyRedisPort = config.getInt("proxy.redis.port", 6379);
        this.proxyRedisUsername = config.getString("proxy.redis.username", "");
        this.proxyRedisPassword = config.getString("proxy.redis.password", "");
        this.proxyRedisTimeout = Math.max(1, config.getInt("proxy.redis.timeout-seconds", 2));
        this.proxyRedisChannel = config.getString("proxy.redis.channel", "skyblock:proxy");
    }

    public String getWorldName() {
        return worldName;
    }

    public int getIslandHeight() {
        return islandHeight;
    }

    public int getIslandDistance() {
        return islandDistance;
    }

    public int getIslandSize() {
        return islandSize;
    }

    public boolean isBorderEnabled() {
        return borderEnabled;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public Location getSpawnLocation() {
        if (spawnWorld == null || spawnWorld.isEmpty())
            return null;
        World world = Bukkit.getWorld(spawnWorld);
        if (world == null)
            return null;
        return new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    public int getMaxConcurrentCreations() {
        return maxConcurrentCreations;
    }

    public int getCreationThreads() {
        return creationThreads;
    }

    public String getCommandName() {
        return commandName;
    }

    public List<String> getCommandAliases() {
        return commandAliases;
    }

    public Map<String, List<String>> getSubcommandAliases() {
        return subcommandAliases;
    }

    public int getInviteExpireSeconds() {
        return inviteExpireSeconds;
    }

    public boolean isEconomyEnabled() {
        return economyEnabled;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public boolean isProxyEnabled() {
        return proxyEnabled;
    }

    public boolean isProxyDebug() {
        return proxyDebug;
    }

    public String getProxyServerName() {
        return proxyServerName;
    }

    public String getProxySpawnServer() {
        return proxySpawnServer;
    }

    public List<String> getProxyCreateServers() {
        return proxyCreateServers;
    }

    public int getProxyPendingSeconds() {
        return proxyPendingSeconds;
    }

    public String getProxyRedisHost() {
        return proxyRedisHost;
    }

    public int getProxyRedisPort() {
        return proxyRedisPort;
    }

    public String getProxyRedisUsername() {
        return proxyRedisUsername;
    }

    public String getProxyRedisPassword() {
        return proxyRedisPassword;
    }

    public int getProxyRedisTimeout() {
        return proxyRedisTimeout;
    }

    public String getProxyRedisChannel() {
        return proxyRedisChannel;
    }
}
