package net.cengiz1.skyblock.upgrade;

import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

public class UpgradeLevel {

    private final int level;
    private final double value;
    private final int requiredIslandLevel;
    private final double requiredMoney;
    private final Material icon;
    private final Map<Material, Integer> chances;

    public UpgradeLevel(int level, double value, int requiredIslandLevel, double requiredMoney,
                        Material icon, Map<Material, Integer> chances) {
        this.level = level;
        this.value = value;
        this.requiredIslandLevel = requiredIslandLevel;
        this.requiredMoney = requiredMoney;
        this.icon = icon;
        this.chances = chances == null ? Collections.emptyMap() : chances;
    }

    public int getLevel() {
        return level;
    }

    public double getValue() {
        return value;
    }

    public int getRequiredIslandLevel() {
        return requiredIslandLevel;
    }

    public double getRequiredMoney() {
        return requiredMoney;
    }

    public Material getIcon() {
        return icon;
    }

    public Map<Material, Integer> getChances() {
        return chances;
    }
}
