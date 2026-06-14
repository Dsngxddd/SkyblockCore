package net.cengiz1.skyblock.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ConfigMigrator {

    private ConfigMigrator() {
    }

    public static void sync(JavaPlugin plugin, String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
            return;
        }

        InputStream resource = plugin.getResource(resourcePath);
        if (resource == null)
            return;

        YamlConfiguration defaults;
        try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException error) {
            plugin.getLogger().warning("Could not read bundled resource " + resourcePath + ": " + error.getMessage());
            return;
        }

        YamlConfiguration current = YamlConfiguration.loadConfiguration(file);

        boolean changed = false;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key))
                continue;
            if (!current.contains(key)) {
                current.set(key, defaults.get(key));
                changed = true;
            }
        }

        if (!changed)
            return;

        try {
            current.save(file);
            plugin.getLogger().info("Updated " + resourcePath + " with new default keys.");
        } catch (IOException error) {
            plugin.getLogger().warning("Could not update " + resourcePath + ": " + error.getMessage());
        }
    }
}
