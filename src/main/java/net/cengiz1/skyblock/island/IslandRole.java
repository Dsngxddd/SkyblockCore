package net.cengiz1.skyblock.island;

import java.util.EnumSet;
import java.util.Set;

public enum IslandRole {

    VISITOR(0, "&7Visitor"),
    FARMER(1, "&aFarmer", IslandPermission.FARM, IslandPermission.INTERACT),
    MEMBER(2, "&fMember",
            IslandPermission.BLOCK_PLACE, IslandPermission.BLOCK_BREAK, IslandPermission.INTERACT,
            IslandPermission.CONTAINER, IslandPermission.FARM, IslandPermission.PICKUP_ITEMS,
            IslandPermission.DROP_ITEMS, IslandPermission.DAMAGE_MOBS, IslandPermission.FLY,
            IslandPermission.BANK),
    ARCHITECT(3, "&bArchitect",
            IslandPermission.BLOCK_PLACE, IslandPermission.BLOCK_BREAK, IslandPermission.INTERACT,
            IslandPermission.CONTAINER, IslandPermission.FARM, IslandPermission.PICKUP_ITEMS,
            IslandPermission.DROP_ITEMS, IslandPermission.DAMAGE_MOBS, IslandPermission.FLY,
            IslandPermission.BANK, IslandPermission.SET_HOME, IslandPermission.SET_WARP),
    MODERATOR(4, "&eModerator",
            IslandPermission.BLOCK_PLACE, IslandPermission.BLOCK_BREAK, IslandPermission.INTERACT,
            IslandPermission.CONTAINER, IslandPermission.FARM, IslandPermission.PICKUP_ITEMS,
            IslandPermission.DROP_ITEMS, IslandPermission.DAMAGE_MOBS, IslandPermission.FLY,
            IslandPermission.BANK, IslandPermission.SET_HOME, IslandPermission.SET_WARP,
            IslandPermission.INVITE, IslandPermission.KICK, IslandPermission.BAN,
            IslandPermission.TOGGLE_SETTINGS, IslandPermission.UPGRADE, IslandPermission.MANAGE_MEMBERS),
    OWNER(5, "&6Owner", IslandPermission.values());

    private final int weight;
    private String displayName;
    private final EnumSet<IslandPermission> permissions;

    IslandRole(int weight, String displayName, IslandPermission... permissions) {
        this.weight = weight;
        this.displayName = displayName;
        this.permissions = EnumSet.noneOf(IslandPermission.class);
        for (IslandPermission permission : permissions)
            this.permissions.add(permission);
    }

    public int getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName != null && !displayName.isEmpty())
            this.displayName = displayName;
    }

    public Set<IslandPermission> getPermissions() {
        return permissions;
    }

    public boolean has(IslandPermission permission) {
        return this.permissions.contains(permission);
    }

    public void setPermissions(Set<IslandPermission> values) {
        if (this == OWNER)
            return;
        this.permissions.clear();
        this.permissions.addAll(values);
    }

    public boolean canManage(IslandRole target) {
        return this.weight > target.weight;
    }

    public static IslandRole[] assignable() {
        return new IslandRole[]{FARMER, MEMBER, ARCHITECT, MODERATOR};
    }

    public static IslandRole fromString(String name) {
        if (name == null)
            return null;
        try {
            return valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException error) {
            return null;
        }
    }
}
