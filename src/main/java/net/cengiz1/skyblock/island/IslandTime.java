package net.cengiz1.skyblock.island;

/**
 * Ada zaman ayarı. NORMAL döngüye bırakır; diğerleri adada saati sabitler.
 * Sabitleme yetkisi izin gerektirir (örn. skyblock.time).
 */
public enum IslandTime {

    NORMAL(-1, "&fNormal (Döngü)"),
    DAY(1000, "&eGündüz"),
    NOON(6000, "&6Öğlen"),
    NIGHT(13000, "&9Gece"),
    MIDNIGHT(18000, "&5Gece Yarısı");

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
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException error) {
            return NORMAL;
        }
    }
}
