package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.config.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ada komutunu config'deki ad ve alias'larla dinamik olarak Bukkit'in
 * CommandMap'ine kaydeder (plugin.yml'e bağlı kalmadan).
 */
public final class CommandRegistrar {

    private CommandRegistrar() {
    }

    public static void register(SkyblockPlugin plugin) {
        SettingsManager settings = plugin.getSettings();

        // alias -> kanonik alt komut çözümleyicisi
        Map<String, String> resolver = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : settings.getSubcommandAliases().entrySet()) {
            String canonical = entry.getKey();
            for (String alias : entry.getValue())
                resolver.put(alias.toLowerCase(), canonical);
        }

        IslandCommand command = new IslandCommand(plugin,
                settings.getCommandName(), settings.getCommandAliases(), resolver);

        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            plugin.getLogger().severe("CommandMap alınamadı; ada komutu kaydedilemedi!");
            return;
        }
        commandMap.register(plugin.getName().toLowerCase(), command);
        plugin.getLogger().info("Ada komutu kaydedildi: /" + settings.getCommandName()
                + " (" + String.join(", ", settings.getCommandAliases()) + ")");
    }

    private static CommandMap getCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (ReflectiveOperationException error) {
            return null;
        }
    }
}
