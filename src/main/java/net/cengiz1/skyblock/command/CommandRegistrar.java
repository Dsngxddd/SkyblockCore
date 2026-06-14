package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.config.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CommandRegistrar {

    private CommandRegistrar() {
    }

    public static void register(SkyblockPlugin plugin) {
        SettingsManager settings = plugin.getSettings();

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
            plugin.getLogger().severe("Could not access the CommandMap; the island command was not registered!");
            return;
        }
        commandMap.register(plugin.getName().toLowerCase(), command);
        plugin.getLogger().info("Registered island command: /" + settings.getCommandName()
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
