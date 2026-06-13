package net.cengiz1.skyblock.config;

import net.cengiz1.skyblock.SkyblockPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class SettingsManager {

    private final SkyblockPlugin plugin;

    private String worldName;
    private int islandHeight;
    private int islandDistance;
    private int islandSize;

    private int maxConcurrentCreations;
    private int creationThreads;

    private String storageType;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private boolean useSsl;

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

        this.maxConcurrentCreations = Math.max(1, config.getInt("creation.max-concurrent", 3));
        this.creationThreads = Math.max(1, config.getInt("creation.threads", 3));

        this.storageType = config.getString("storage.type", "sqlite").toLowerCase();
        this.host = config.getString("storage.mysql.host", "localhost");
        this.port = config.getInt("storage.mysql.port", 3306);
        this.database = config.getString("storage.mysql.database", "skyblock");
        this.username = config.getString("storage.mysql.username", "root");
        this.password = config.getString("storage.mysql.password", "");
        this.useSsl = config.getBoolean("storage.mysql.ssl", false);
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

    public int getMaxConcurrentCreations() {
        return maxConcurrentCreations;
    }

    public int getCreationThreads() {
        return creationThreads;
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
}
