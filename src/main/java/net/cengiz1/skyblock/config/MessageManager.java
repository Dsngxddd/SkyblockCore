package net.cengiz1.skyblock.config;

import net.cengiz1.skyblock.SkyblockPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final SkyblockPlugin plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String prefix = "";

    public MessageManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.messages.clear();

        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.prefix = color(config.getString("prefix", ""));

        ConfigurationSection section = config.getConfigurationSection("messages");
        if (section != null) {
            for (String key : section.getKeys(false))
                this.messages.put(key, section.getString(key));
        }
    }

    public String get(String key, String... replacements) {
        String message = this.messages.getOrDefault(key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2)
            message = message.replace(replacements[i], replacements[i + 1]);
        return color(message);
    }

    public void send(CommandSender sender, String key, String... replacements) {
        String message = get(key, replacements);
        if (!message.isEmpty())
            sender.sendMessage(this.prefix + message);
    }

    private String color(String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }
}
