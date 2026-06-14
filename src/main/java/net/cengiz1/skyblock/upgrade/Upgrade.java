package net.cengiz1.skyblock.upgrade;

import org.bukkit.Material;

import java.util.TreeMap;

public class Upgrade {

    private final String key;
    private final String displayName;
    private final Material icon;
    private final int slot;
    private final UpgradeType type;
    private final TreeMap<Integer, UpgradeLevel> levels = new TreeMap<>();

    public Upgrade(String key, String displayName, Material icon, int slot, UpgradeType type) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.slot = slot;
        this.type = type;
    }

    public void addLevel(UpgradeLevel level) {
        this.levels.put(level.getLevel(), level);
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getSlot() {
        return slot;
    }

    public UpgradeType getType() {
        return type;
    }

    public int getMaxLevel() {
        return this.levels.isEmpty() ? 1 : this.levels.lastKey();
    }

    public UpgradeLevel getLevel(int level) {
        UpgradeLevel exact = this.levels.get(level);
        if (exact != null)
            return exact;
        java.util.Map.Entry<Integer, UpgradeLevel> floor = this.levels.floorEntry(level);
        if (floor != null)
            return floor.getValue();
        return this.levels.isEmpty() ? null : this.levels.firstEntry().getValue();
    }

    public UpgradeLevel getNextLevel(int currentLevel) {
        java.util.Map.Entry<Integer, UpgradeLevel> next = this.levels.higherEntry(currentLevel);
        return next == null ? null : next.getValue();
    }

    public boolean hasNext(int currentLevel) {
        return this.levels.higherKey(currentLevel) != null;
    }
}
