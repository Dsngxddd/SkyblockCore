package net.cengiz1.skyblock.upgrade;

public enum UpgradeType {
    VALUE,
    GENERATOR;

    public static UpgradeType fromString(String name) {
        if (name == null)
            return VALUE;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException error) {
            return VALUE;
        }
    }
}
