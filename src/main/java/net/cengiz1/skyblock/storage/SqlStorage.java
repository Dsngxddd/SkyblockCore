package net.cengiz1.skyblock.storage;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.config.SettingsManager;
import net.cengiz1.skyblock.island.Island;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class SqlStorage implements Storage {

    private final SkyblockPlugin plugin;
    private final SettingsManager settings;
    private final boolean mysql;

    private Connection connection;

    public SqlStorage(SkyblockPlugin plugin, SettingsManager settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.mysql = settings.getStorageType().equals("mysql");
    }

    @Override
    public void init() throws Exception {
        openConnection();
        try (PreparedStatement statement = this.connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS islands (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "owner VARCHAR(36) NOT NULL," +
                        "world VARCHAR(64) NOT NULL," +
                        "grid_index INT NOT NULL," +
                        "center_x INT NOT NULL," +
                        "center_y INT NOT NULL," +
                        "center_z INT NOT NULL," +
                        "home_x DOUBLE NOT NULL," +
                        "home_y DOUBLE NOT NULL," +
                        "home_z DOUBLE NOT NULL," +
                        "home_yaw FLOAT NOT NULL," +
                        "home_pitch FLOAT NOT NULL," +
                        "flags TEXT)")) {
            statement.executeUpdate();
        }
        addColumnIfMissing("flags", "TEXT");
    }

    private void addColumnIfMissing(String column, String type) {
        try (PreparedStatement statement = this.connection.prepareStatement(
                "ALTER TABLE islands ADD COLUMN " + column + " " + type)) {
            statement.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    private void openConnection() throws SQLException {
        try {
            if (this.mysql)
                Class.forName("com.mysql.cj.jdbc.Driver");
            else
                Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException error) {
            throw new SQLException("JDBC driver not found: " + error.getMessage());
        }

        if (this.mysql) {
            String url = "jdbc:mysql://" + settings.getHost() + ":" + settings.getPort() + "/" + settings.getDatabase()
                    + "?useSSL=" + settings.isUseSsl() + "&autoReconnect=true&characterEncoding=utf8";
            this.connection = DriverManager.getConnection(url, settings.getUsername(), settings.getPassword());
        } else {
            File file = new File(plugin.getDataFolder(), "data.db");
            file.getParentFile().mkdirs();
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        }
    }

    private Connection connection() throws SQLException {
        if (this.connection == null || this.connection.isClosed())
            openConnection();
        return this.connection;
    }

    @Override
    public synchronized Collection<Island> loadAll() {
        List<Island> islands = new LinkedList<>();
        try (PreparedStatement statement = connection().prepareStatement("SELECT * FROM islands");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Island island = new Island(
                        UUID.fromString(result.getString("uuid")),
                        UUID.fromString(result.getString("owner")),
                        result.getString("world"),
                        result.getInt("grid_index"),
                        result.getInt("center_x"),
                        result.getInt("center_y"),
                        result.getInt("center_z"));
                island.setHome(
                        result.getDouble("home_x"),
                        result.getDouble("home_y"),
                        result.getDouble("home_z"),
                        result.getFloat("home_yaw"),
                        result.getFloat("home_pitch"));
                island.loadFlags(result.getString("flags"));
                island.markClean();
                islands.add(island);
            }
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not load islands: " + error.getMessage());
        }
        return islands;
    }

    @Override
    public synchronized void save(Island island) {
        String sql = this.mysql
                ? "INSERT INTO islands (uuid, owner, world, grid_index, center_x, center_y, center_z, home_x, home_y, home_z, home_yaw, home_pitch, flags) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                "owner=?, world=?, grid_index=?, center_x=?, center_y=?, center_z=?, home_x=?, home_y=?, home_z=?, home_yaw=?, home_pitch=?, flags=?"
                : "INSERT INTO islands (uuid, owner, world, grid_index, center_x, center_y, center_z, home_x, home_y, home_z, home_yaw, home_pitch, flags) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET " +
                "owner=?, world=?, grid_index=?, center_x=?, center_y=?, center_z=?, home_x=?, home_y=?, home_z=?, home_yaw=?, home_pitch=?, flags=?";

        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, island.getUniqueId().toString());
            statement.setString(2, island.getOwner().toString());
            statement.setString(3, island.getWorldName());
            statement.setInt(4, island.getGridIndex());
            statement.setInt(5, island.getCenterX());
            statement.setInt(6, island.getCenterY());
            statement.setInt(7, island.getCenterZ());
            statement.setDouble(8, island.getHomeX());
            statement.setDouble(9, island.getHomeY());
            statement.setDouble(10, island.getHomeZ());
            statement.setFloat(11, island.getHomeYaw());
            statement.setFloat(12, island.getHomePitch());
            statement.setString(13, island.serializeFlags());

            statement.setString(14, island.getOwner().toString());
            statement.setString(15, island.getWorldName());
            statement.setInt(16, island.getGridIndex());
            statement.setInt(17, island.getCenterX());
            statement.setInt(18, island.getCenterY());
            statement.setInt(19, island.getCenterZ());
            statement.setDouble(20, island.getHomeX());
            statement.setDouble(21, island.getHomeY());
            statement.setDouble(22, island.getHomeZ());
            statement.setFloat(23, island.getHomeYaw());
            statement.setFloat(24, island.getHomePitch());
            statement.setString(25, island.serializeFlags());

            statement.executeUpdate();
            island.markClean();
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not save island " + island.getUniqueId() + ": " + error.getMessage());
        }
    }

    @Override
    public synchronized void delete(UUID islandId) {
        try (PreparedStatement statement = connection().prepareStatement("DELETE FROM islands WHERE uuid = ?")) {
            statement.setString(1, islandId.toString());
            statement.executeUpdate();
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not delete island " + islandId + ": " + error.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (this.connection != null && !this.connection.isClosed())
                this.connection.close();
        } catch (SQLException ignored) {
        }
    }
}
