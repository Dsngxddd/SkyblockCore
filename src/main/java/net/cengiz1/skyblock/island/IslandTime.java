package net.cengiz1.skyblock.island;

public enum IslandTime {

    NORMAL(-1, "&fNormal (Cycle)"),
    DAY(1000, "&eDay"),
    NOON(6000, "&6Noon"),
    NIGHT(13000, "&9Night"),
    MIDNIGHT(18000, "&5Midnight");

    private final long ticks;
    private final String displayName;

    IslandTime(long ticks, String displayName) {
        this.ticks = ticks;
        this.displayName = displayName;
    }

    public long getTicks() {
        return ticks;
    }

    public boolean isFixed() {
        return ticks >= 0;
    }

    public String getDisplayName() {
        return displayName;
    }

    public IslandTime next() {
        IslandTime[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static IslandTime fromString(String name) {
        if (name == null)
            return NORMAL;
        try {
            return valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException error) {
            return NORMAL;
        }
    }
}
