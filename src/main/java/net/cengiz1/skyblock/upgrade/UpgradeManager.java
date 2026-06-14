package net.cengiz1.skyblock.upgrade;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.economy.EconomyHook;
import net.cengiz1.skyblock.island.Island;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class UpgradeManager {

    public enum PurchaseResult {
        SUCCESS,
        MAX_LEVEL,
        UNKNOWN_UPGRADE,
        NEED_ISLAND_LEVEL,
        NEED_MONEY,
        NO_PERMISSION
    }

    private final SkyblockPlugin plugin;
    private final Map<String, Upgrade> upgrades = new LinkedHashMap<>();

    public UpgradeManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.upgrades.clear();

        File file = new File(plugin.getDataFolder(), "upgrades.yml");
        if (!file.exists())
            plugin.saveResource("upgrades.yml", false);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("upgrades");
        if (root == null)
            return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null)
                continue;

            String displayName = section.getString("display-name", key);
            Material icon = matchMaterial(section.getString("icon", "PAPER"), Material.PAPER);
            int slot = section.getInt("slot", 0);
            UpgradeType type = UpgradeType.fromString(section.getString("type", "value"));

            Upgrade upgrade = new Upgrade(key.toLowerCase(), displayName, icon, slot, type);

            ConfigurationSection levels = section.getConfigurationSection("levels");
            if (levels != null) {
                for (String levelKey : levels.getKeys(false)) {
                    int level;
                    try {
                        level = Integer.parseInt(levelKey);
                    } catch (NumberFormatException error) {
                        continue;
                    }
                    ConfigurationSection ls = levels.getConfigurationSection(levelKey);
                    if (ls == null)
                        continue;

                    double value = ls.getDouble("value", 0);
                    int reqLevel = ls.getInt("required-level", 0);
                    double reqMoney = ls.getDouble("required-money", 0);
                    Material levelIcon = ls.contains("icon") ? matchMaterial(ls.getString("icon"), icon) : null;

                    Map<Material, Integer> chances = Collections.emptyMap();
                    if (type == UpgradeType.GENERATOR) {
                        chances = new LinkedHashMap<>();
                        ConfigurationSection cs = ls.getConfigurationSection("chances");
                        if (cs != null) {
                            for (String matKey : cs.getKeys(false)) {
                                Material mat = matchMaterial(matKey, null);
                                if (mat != null)
                                    chances.put(mat, Math.max(0, cs.getInt(matKey)));
                            }
                        }
                    }

                    upgrade.addLevel(new UpgradeLevel(level, value, reqLevel, reqMoney, levelIcon, chances));
                }
            }

            this.upgrades.put(upgrade.getKey(), upgrade);
        }
        plugin.getLogger().info("Loaded " + this.upgrades.size() + " upgrades.");
    }

    public Upgrade get(String key) {
        return key == null ? null : this.upgrades.get(key.toLowerCase());
    }

    public Map<String, Upgrade> getUpgrades() {
        return Collections.unmodifiableMap(this.upgrades);
    }

    public double getValue(Island island, String key, double fallback) {
        Upgrade upgrade = get(key);
        if (upgrade == null)
            return fallback;
        UpgradeLevel level = upgrade.getLevel(island.getUpgradeLevel(key));
        return level == null ? fallback : level.getValue();
    }

    public Material pickGeneratorBlock(Island island, String key, Material fallback) {
        Upgrade upgrade = get(key);
        if (upgrade == null || upgrade.getType() != UpgradeType.GENERATOR)
            return fallback;
        UpgradeLevel level = upgrade.getLevel(island.getUpgradeLevel(key));
        if (level == null || level.getChances().isEmpty())
            return fallback;

        int total = 0;
        for (int weight : level.getChances().values())
            total += weight;
        if (total <= 0)
            return fallback;

        int roll = ThreadLocalRandom.current().nextInt(total);
        for (Map.Entry<Material, Integer> entry : level.getChances().entrySet()) {
            roll -= entry.getValue();
            if (roll < 0)
                return entry.getKey();
        }
        return fallback;
    }

    public PurchaseResult purchase(Player player, Island island, String key, EconomyHook economy) {
        Upgrade upgrade = get(key);
        if (upgrade == null)
            return PurchaseResult.UNKNOWN_UPGRADE;

        int current = island.getUpgradeLevel(key);
        UpgradeLevel next = upgrade.getNextLevel(current);
        if (next == null)
            return PurchaseResult.MAX_LEVEL;

        if (island.getLevel() < next.getRequiredIslandLevel())
            return PurchaseResult.NEED_ISLAND_LEVEL;

        double cost = next.getRequiredMoney();
        if (economy.isEnabled() && cost > 0 && !economy.has(player, cost))
            return PurchaseResult.NEED_MONEY;

        if (economy.isEnabled() && cost > 0 && !economy.withdraw(player, cost))
            return PurchaseResult.NEED_MONEY;

        island.setUpgradeLevel(key, next.getLevel());
        plugin.getIslandManager().saveAsync(island);

        if (key.equalsIgnoreCase("size") && plugin.getIslandManager().getBorderManager() != null)
            plugin.getIslandManager().getBorderManager().refresh(island);
        return PurchaseResult.SUCCESS;
    }

    private Material matchMaterial(String name, Material fallback) {
        if (name == null)
            return fallback;
        Material material = Material.matchMaterial(name);
        return material != null ? material : fallback;
    }
}
