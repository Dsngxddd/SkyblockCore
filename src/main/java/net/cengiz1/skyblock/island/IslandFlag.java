package net.cengiz1.skyblock.island;

public enum IslandFlag {

    PVP(false),
    MOB_SPAWNING(true),
    CREEPER_EXPLOSION(false),
    TNT_EXPLOSION(false),
    FIRE_SPREAD(false),
    VISITOR_INTERACT(false);

    private final boolean defaultValue;

    IslandFlag(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean getDefault() {
        return defaultValue;
    }

    public static IslandFlag fromString(String name) {
        if (name == null)
            return null;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException error) {
            return null;
        }
    }
}
