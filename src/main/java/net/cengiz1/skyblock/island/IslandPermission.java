package net.cengiz1.skyblock.island;

public enum IslandPermission {

    BLOCK_PLACE,
    BLOCK_BREAK,
    INTERACT,
    CONTAINER,
    FARM,
    PICKUP_ITEMS,
    DROP_ITEMS,
    DAMAGE_MOBS,
    INVITE,
    KICK,
    BAN,
    SET_HOME,
    SET_WARP,
    TOGGLE_SETTINGS,
    UPGRADE,
    MANAGE_MEMBERS,
    FLY,
    DELETE_ISLAND,
    TRANSFER;

    public static IslandPermission fromString(String name) {
        if (name == null)
            return null;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException error) {
            return null;
        }
    }
}
