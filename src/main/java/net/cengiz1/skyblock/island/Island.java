package net.cengiz1.skyblock.island;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Island {

    private final UUID uniqueId;
    private UUID owner;
    private final String worldName;
    private final int gridIndex;
    private final int centerX;
    private final int centerY;
    private final int centerZ;

    private double homeX;
    private double homeY;
    private double homeZ;
    private float homeYaw;
    private float homePitch;

    private String name;
    private boolean locked;
    private IslandTime time = IslandTime.NORMAL;

    private String serverName;

    private double points;
    private int level;

    private final EnumMap<IslandFlag, Boolean> flags = new EnumMap<>(IslandFlag.class);
    private final Map<UUID, IslandRole> members = new ConcurrentHashMap<>();
    private final Set<UUID> banned = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> upgrades = new ConcurrentHashMap<>();

    private boolean dirty;

    public Island(UUID uniqueId, UUID owner, String worldName, int gridIndex,
                  int centerX, int centerY, int centerZ) {
        this.uniqueId = uniqueId;
        this.owner = owner;
        this.worldName = worldName;
        this.gridIndex = gridIndex;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.homeX = centerX + 0.5;
        this.homeY = centerY + 1;
        this.homeZ = centerZ + 0.5;
        for (IslandFlag flag : IslandFlag.values())
            this.flags.put(flag, flag.getDefault());
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.dirty = true;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getGridIndex() {
        return gridIndex;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public Location getCenter(World world) {
        return new Location(world, centerX + 0.5, centerY, centerZ + 0.5);
    }

    public Location getHome(World world) {
        return new Location(world, homeX, homeY, homeZ, homeYaw, homePitch);
    }

    public void setHome(Location location) {
        this.homeX = location.getX();
        this.homeY = location.getY();
        this.homeZ = location.getZ();
        this.homeYaw = location.getYaw();
        this.homePitch = location.getPitch();
        this.dirty = true;
    }

    public void setHome(double x, double y, double z, float yaw, float pitch) {
        this.homeX = x;
        this.homeY = y;
        this.homeZ = z;
        this.homeYaw = yaw;
        this.homePitch = pitch;
    }

    public double getHomeX() {
        return homeX;
    }

    public double getHomeY() {
        return homeY;
    }

    public double getHomeZ() {
        return homeZ;
    }

    public float getHomeYaw() {
        return homeYaw;
    }

    public float getHomePitch() {
        return homePitch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.dirty = true;
    }

    public void setNameRaw(String name) {
        this.name = name;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        this.dirty = true;
    }

    public void setLockedRaw(boolean locked) {
        this.locked = locked;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
        this.dirty = true;
    }

    public void setServerNameRaw(String serverName) {
        this.serverName = serverName;
    }

    public IslandTime getTime() {
        return time;
    }

    public void setTime(IslandTime time) {
        this.time = time == null ? IslandTime.NORMAL : time;
        this.dirty = true;
    }

    public void setTimeRaw(IslandTime time) {
        this.time = time == null ? IslandTime.NORMAL : time;
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = Math.max(0, points);
        this.dirty = true;
    }

    public void setPointsRaw(double points) {
        this.points = Math.max(0, points);
    }

    public void addPoints(double amount) {
        setPoints(this.points + amount);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(0, level);
        this.dirty = true;
    }

    public void setLevelRaw(int level) {
        this.level = Math.max(0, level);
    }

    public boolean getFlag(IslandFlag flag) {
        return this.flags.getOrDefault(flag, flag.getDefault());
    }

    public void setFlag(IslandFlag flag, boolean value) {
        this.flags.put(flag, value);
        this.dirty = true;
    }

    public Map<UUID, IslandRole> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public Set<UUID> getAllMemberIds() {
        Set<UUID> ids = new HashSet<>(members.keySet());
        ids.add(owner);
        return ids;
    }

    public boolean isOwner(UUID id) {
        return owner.equals(id);
    }

    public boolean isMember(UUID id) {
        return owner.equals(id) || members.containsKey(id);
    }

    public IslandRole getRole(UUID id) {
        if (owner.equals(id))
            return IslandRole.OWNER;
        IslandRole role = members.get(id);
        return role != null ? role : IslandRole.VISITOR;
    }

    public void setRole(UUID id, IslandRole role) {
        if (owner.equals(id) || role == null || role == IslandRole.VISITOR)
            return;
        this.members.put(id, role);
        this.dirty = true;
    }

    public void addMember(UUID id) {
        setRole(id, IslandRole.MEMBER);
    }

    public void removeMember(UUID id) {
        if (this.members.remove(id) != null)
            this.dirty = true;
    }

    public boolean hasPermission(UUID id, IslandPermission permission) {
        return getRole(id).has(permission);
    }

    public int getMemberCount() {
        return members.size() + 1;
    }

    public Set<UUID> getBanned() {
        return Collections.unmodifiableSet(banned);
    }

    public boolean isBanned(UUID id) {
        return banned.contains(id);
    }

    public void ban(UUID id) {
        if (banned.add(id)) {
            members.remove(id);
            this.dirty = true;
        }
    }

    public void unban(UUID id) {
        if (banned.remove(id))
            this.dirty = true;
    }

    public int getUpgradeLevel(String key) {
        return this.upgrades.getOrDefault(key.toLowerCase(), 1);
    }

    public void setUpgradeLevel(String key, int level) {
        this.upgrades.put(key.toLowerCase(), Math.max(1, level));
        this.dirty = true;
    }

    public Map<String, Integer> getUpgrades() {
        return Collections.unmodifiableMap(upgrades);
    }

    public String serializeFlags() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<IslandFlag, Boolean> entry : this.flags.entrySet()) {
            if (builder.length() > 0)
                builder.append(';');
            builder.append(entry.getKey().name()).append('=').append(entry.getValue() ? 1 : 0);
        }
        return builder.toString();
    }

    public void loadFlags(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return;
        for (String part : serialized.split(";")) {
            String[] keyValue = part.split("=");
            if (keyValue.length != 2)
                continue;
            IslandFlag flag = IslandFlag.fromString(keyValue[0]);
            if (flag != null)
                this.flags.put(flag, keyValue[1].equals("1"));
        }
    }

    public String serializeMembers() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<UUID, IslandRole> entry : this.members.entrySet()) {
            if (builder.length() > 0)
                builder.append(';');
            builder.append(entry.getKey()).append(':').append(entry.getValue().name());
        }
        return builder.toString();
    }

    public void loadMembers(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return;
        for (String part : serialized.split(";")) {
            String[] kv = part.split(":");
            if (kv.length != 2)
                continue;
            try {
                IslandRole role = IslandRole.fromString(kv[1]);
                if (role != null && role != IslandRole.VISITOR)
                    this.members.put(UUID.fromString(kv[0]), role);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public String serializeBanned() {
        StringBuilder builder = new StringBuilder();
        for (UUID id : this.banned) {
            if (builder.length() > 0)
                builder.append(';');
            builder.append(id);
        }
        return builder.toString();
    }

    public void loadBanned(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return;
        for (String part : serialized.split(";")) {
            try {
                this.banned.add(UUID.fromString(part.trim()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public String serializeUpgrades() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : this.upgrades.entrySet()) {
            if (builder.length() > 0)
                builder.append(';');
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    public void loadUpgrades(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return;
        for (String part : serialized.split(";")) {
            String[] kv = part.split("=");
            if (kv.length != 2)
                continue;
            try {
                this.upgrades.put(kv[0].toLowerCase(), Integer.parseInt(kv[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }
}
