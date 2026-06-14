package net.cengiz1.skyblock.level;

import net.cengiz1.skyblock.SkyblockPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BlockValueManager {

    private final SkyblockPlugin plugin;
    private final Map<Material, Double> values = new EnumMap<>(Material.class);
    private double defaultValue;

    public BlockValueManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.values.clear();

        File file = new File(plugin.getDataFolder(), "block-values.yml");
        if (!file.exists())
            plugin.saveResource("block-values.yml", false);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.defaultValue = config.getDouble("default-value", 0.0);

        ConfigurationSection section = config.getConfigurationSection("values");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material == null) {
                    plugin.getLogger().warning("block-values.yml: unknown block '" + key + "'");
                    continue;
                }
                this.values.put(material, section.getDouble(key));
            }
        }
        plugin.getLogger().info("Loaded " + this.values.size() + " block values.");
    }

    public double getValue(Material material) {
        if (material == null)
            return 0.0;
        return this.values.getOrDefault(material, this.defaultValue);
    }

    public boolean isTracked(Material material) {
        return getValue(material) != 0.0;
    }

    public Map<Material, Double> getPositiveValues() {
        Map<Material, Double> result = new LinkedHashMap<>();
        for (Map.Entry<Material, Double> entry : this.values.entrySet())
            if (entry.getValue() > 0)
                result.put(entry.getKey(), entry.getValue());
        return result;
    }
}
