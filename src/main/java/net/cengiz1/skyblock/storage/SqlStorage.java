package net.cengiz1.skyblock.storage;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.config.SettingsManager;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandTime;

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
        addColumnIfMissing("name", "VARCHAR(64)");
        addColumnIfMissing("locked", "INT");
        addColumnIfMissing("island_time", "VARCHAR(32)");
        addColumnIfMissing("points", "DOUBLE");
        addColumnIfMissing("level", "INT");
        addColumnIfMissing("members", "TEXT");
        addColumnIfMissing("banned", "TEXT");
        addColumnIfMissing("upgrades", "TEXT");
        addColumnIfMissing("server", "VARCHAR(64)");
        addColumnIfMissing("has_warp", "INT");
        addColumnIfMissing("warp_x", "DOUBLE");
        addColumnIfMissing("warp_y", "DOUBLE");
        addColumnIfMissing("warp_z", "DOUBLE");
        addColumnIfMissing("warp_yaw", "FLOAT");
        addColumnIfMissing("warp_pitch", "FLOAT");
        addColumnIfMissing("border_color", "VARCHAR(16)");
        addColumnIfMissing("bank", "DOUBLE");
        addColumnIfMissing("warps", "TEXT");
        addColumnIfMissing("custom_roles", "TEXT");
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
            while (result.next())
                islands.add(mapRow(result));
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not load islands: " + error.getMessage());
        }
        return islands;
    }

    @Override
    public synchronized Island load(UUID islandId) {
        try (PreparedStatement statement = connection().prepareStatement("SELECT * FROM islands WHERE uuid = ?")) {
            statement.setString(1, islandId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next())
                    return mapRow(result);
            }
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not load island " + islandId + ": " + error.getMessage());
        }
        return null;
    }

    private Island mapRow(ResultSet result) throws SQLException {
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

        island.setNameRaw(result.getString("name"));
        island.setLockedRaw(result.getInt("locked") == 1);
        island.setTimeRaw(IslandTime.fromString(result.getString("island_time")));
        island.setPointsRaw(result.getDouble("points"));
        island.setLevelRaw(result.getInt("level"));
        island.loadMembers(result.getString("members"));
        island.loadCustomRoles(result.getString("custom_roles"));
        island.loadBanned(result.getString("banned"));
        island.loadUpgrades(result.getString("upgrades"));
        island.setServerNameRaw(result.getString("server"));
        String warpsData = result.getString("warps");
        if (warpsData != null && !warpsData.isEmpty()) {
            island.loadWarps(warpsData);
        } else {
            island.setWarpRaw(
                    result.getInt("has_warp") == 1,
                    result.getDouble("warp_x"),
                    result.getDouble("warp_y"),
                    result.getDouble("warp_z"),
                    result.getFloat("warp_yaw"),
                    result.getFloat("warp_pitch"));
        }
        island.setBorderColorRaw(result.getString("border_color"));
        island.setBankRaw(result.getDouble("bank"));

        island.markClean();
        return island;
    }

    @Override
    public synchronized void save(Island island) {
        String columns = "uuid, owner, world, grid_index, center_x, center_y, center_z, " +
                "home_x, home_y, home_z, home_yaw, home_pitch, flags, " +
                "name, locked, island_time, points, level, members, banned, upgrades, server, " +
                "has_warp, warp_x, warp_y, warp_z, warp_yaw, warp_pitch, border_color, bank, warps, custom_roles";
        String placeholders = "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?";
        String updateAssignments = "owner=?, world=?, grid_index=?, center_x=?, center_y=?, center_z=?, " +
                "home_x=?, home_y=?, home_z=?, home_yaw=?, home_pitch=?, flags=?, " +
                "name=?, locked=?, island_time=?, points=?, level=?, members=?, banned=?, upgrades=?, server=?, " +
                "has_warp=?, warp_x=?, warp_y=?, warp_z=?, warp_yaw=?, warp_pitch=?, border_color=?, bank=?, warps=?, custom_roles=?";

        String sql = this.mysql
                ? "INSERT INTO islands (" + columns + ") VALUES (" + placeholders + ") " +
                "ON DUPLICATE KEY UPDATE " + updateAssignments
                : "INSERT INTO islands (" + columns + ") VALUES (" + placeholders + ") " +
                "ON CONFLICT(uuid) DO UPDATE SET " + updateAssignments;

        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            int i = 1;

            statement.setString(i++, island.getUniqueId().toString());
            i = bindCommon(statement, island, i);

            bindCommon(statement, island, i);

            statement.executeUpdate();
            island.markClean();
        } catch (SQLException error) {
            plugin.getLogger().warning("Could not save island " + island.getUniqueId() + ": " + error.getMessage());
        }
    }

    private int bindCommon(PreparedStatement statement, Island island, int i) throws SQLException {
        statement.setString(i++, island.getOwner().toString());
        statement.setString(i++, island.getWorldName());
        statement.setInt(i++, island.getGridIndex());
        statement.setInt(i++, island.getCenterX());
        statement.setInt(i++, island.getCenterY());
        statement.setInt(i++, island.getCenterZ());
        statement.setDouble(i++, island.getHomeX());
        statement.setDouble(i++, island.getHomeY());
        statement.setDouble(i++, island.getHomeZ());
        statement.setFloat(i++, island.getHomeYaw());
        statement.setFloat(i++, island.getHomePitch());
        statement.setString(i++, island.serializeFlags());
        statement.setString(i++, island.getName());
        statement.setInt(i++, island.isLocked() ? 1 : 0);
        statement.setString(i++, island.getTime().name());
        statement.setDouble(i++, island.getPoints());
        statement.setInt(i++, island.getLevel());
        statement.setString(i++, island.serializeMembers());
        statement.setString(i++, island.serializeBanned());
        statement.setString(i++, island.serializeUpgrades());
        statement.setString(i++, island.getServerName());
        statement.setInt(i++, island.hasWarp() ? 1 : 0);
        statement.setDouble(i++, island.getWarpX());
        statement.setDouble(i++, island.getWarpY());
        statement.setDouble(i++, island.getWarpZ());
        statement.setFloat(i++, island.getWarpYaw());
        statement.setFloat(i++, island.getWarpPitch());
        statement.setString(i++, island.getBorderColor());
        statement.setDouble(i++, island.getBank());
        statement.setString(i++, island.serializeWarps());
        statement.setString(i++, island.serializeCustomRoles());
        return i;
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
