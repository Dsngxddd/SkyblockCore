package net.cengiz1.skyblock.schematic;

public class SchematicDefinition {

    private final String key;
    private final String displayName;
    private final String fileName;
    private final double homeOffsetX;
    private final double homeOffsetY;
    private final double homeOffsetZ;

    public SchematicDefinition(String key, String displayName, String fileName,
                               double homeOffsetX, double homeOffsetY, double homeOffsetZ) {
        this.key = key;
        this.displayName = displayName;
        this.fileName = fileName;
        this.homeOffsetX = homeOffsetX;
        this.homeOffsetY = homeOffsetY;
        this.homeOffsetZ = homeOffsetZ;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFileName() {
        return fileName;
    }

    public double getHomeOffsetX() {
        return homeOffsetX;
    }

    public double getHomeOffsetY() {
        return homeOffsetY;
    }

    public double getHomeOffsetZ() {
        return homeOffsetZ;
    }
}
