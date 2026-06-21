package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.config.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
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
                resolver.put(alias.toLowerCase(java.util.Locale.ROOT), canonical);
        }

        IslandCommand command = new IslandCommand(plugin,
                settings.getCommandName(), settings.getCommandAliases(), resolver);

        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            plugin.getLogger().severe("Could not access the CommandMap; the island command was not registered!");
            return;
        }

        unregister(plugin, commandMap);
        commandMap.register(plugin.getName().toLowerCase(java.util.Locale.ROOT), command);
        syncCommands();
        plugin.getLogger().info("Registered island command: /" + settings.getCommandName()
                + " (" + String.join(", ", settings.getCommandAliases()) + ")");
    }

    public static void unregister(SkyblockPlugin plugin) {
        CommandMap commandMap = getCommandMap();
        if (commandMap != null) {
            unregister(plugin, commandMap);
            syncCommands();
        }
    }

    private static void unregister(SkyblockPlugin plugin, CommandMap commandMap) {
        Map<String, Command> known = getKnownCommands(commandMap);
        if (known == null)
            return;
        known.entrySet().removeIf(entry -> {
            if (isOurCommand(entry.getValue())) {
                entry.getValue().unregister(commandMap);
                return true;
            }
            return false;
        });
    }

    private static boolean isOurCommand(Command command) {
        return command != null
                && command.getClass().getName().equals(IslandCommand.class.getName());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> getKnownCommands(CommandMap commandMap) {
        Class<?> type = commandMap.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("knownCommands");
                field.setAccessible(true);
                return (Map<String, Command>) field.get(commandMap);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException error) {
                return null;
            }
        }
        return null;
    }

    private static void syncCommands() {
        try {
            Bukkit.getServer().getClass().getMethod("syncCommands").invoke(Bukkit.getServer());
        } catch (ReflectiveOperationException ignored) {
        }
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
