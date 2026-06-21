package net.cengiz1.skyblock.module;

import net.cengiz1.skyblock.SkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The single handle a module gets to the rest of the server. It exposes the
 * full core API ({@link #getCorePlugin()}), a private data folder, config
 * helpers and listener/command registration that is automatically torn down
 * when the module is disabled.
 */
public final class ModuleContext {

    private final SkyblockPlugin plugin;
    private final String moduleName;
    private final File dataFolder;
    private final ClassLoader classLoader;
    private final Logger logger;

    private final List<Listener> listeners = new ArrayList<>();
    private final List<Command> commands = new ArrayList<>();

    private FileConfiguration config;
    private File configFile;

    ModuleContext(SkyblockPlugin plugin, String moduleName, File dataFolder, ClassLoader classLoader) {
        this.plugin = plugin;
        this.moduleName = moduleName;
        this.dataFolder = dataFolder;
        this.classLoader = classLoader;
        this.logger = Logger.getLogger("Skyblock/" + moduleName);
        this.logger.setParent(plugin.getLogger());
        this.logger.setUseParentHandlers(true);
    }

    /** Full access to the core plugin and all of its managers. */
    public SkyblockPlugin getCorePlugin() {
        return plugin;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getModuleName() {
        return moduleName;
    }

    /** {@code plugins/Skyblock/modules/<name>/} — created on demand. */
    public File getDataFolder() {
        if (!dataFolder.exists())
            dataFolder.mkdirs();
        return dataFolder;
    }

    /** Register a listener owned by the core plugin; removed on module disable. */
    public void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        listeners.add(listener);
    }

    /** Register a command into the live CommandMap; removed on module disable. */
    public void registerCommand(Command command) {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            logger.severe("Could not access the CommandMap; command '" + command.getName() + "' not registered.");
            return;
        }
        commandMap.register(moduleName.toLowerCase(java.util.Locale.ROOT), command);
        commands.add(command);
        syncCommands();
    }

    /**
     * Copy a resource bundled in the module jar into the module data folder.
     * Mirrors {@link org.bukkit.plugin.Plugin#saveResource(String, boolean)}.
     */
    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.isBlank())
            throw new IllegalArgumentException("resourcePath cannot be empty");
        String path = resourcePath.replace('\\', '/');
        File out = new File(getDataFolder(), path);
        if (out.exists() && !replace)
            return;
        File parent = out.getParentFile();
        if (parent != null && !parent.exists())
            parent.mkdirs();
        try (InputStream in = classLoader.getResourceAsStream(path)) {
            if (in == null) {
                logger.warning("Resource '" + path + "' not found in module jar.");
                return;
            }
            Files.copy(in, out.toPath());
        } catch (IOException error) {
            logger.log(Level.SEVERE, "Could not save resource " + path, error);
        }
    }

    /** Lazily loads {@code config.yml} from the module data folder. */
    public FileConfiguration getConfig() {
        if (config == null)
            reloadConfig();
        return config;
    }

    public void reloadConfig() {
        if (configFile == null)
            configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists())
            saveResource("config.yml", false);
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        if (config == null || configFile == null)
            return;
        try {
            config.save(configFile);
        } catch (IOException error) {
            logger.log(Level.SEVERE, "Could not save config.yml", error);
        }
    }

    /** Called by the manager on disable — unregisters everything the module registered. */
    void teardown() {
        for (Listener listener : listeners)
            HandlerList.unregisterAll(listener);
        listeners.clear();

        CommandMap commandMap = getCommandMap();
        if (commandMap != null && !commands.isEmpty()) {
            Map<String, Command> known = getKnownCommands(commandMap);
            for (Command command : commands) {
                command.unregister(commandMap);
                if (known != null)
                    known.values().removeIf(c -> c == command);
            }
            syncCommands();
        }
        commands.clear();
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
}
