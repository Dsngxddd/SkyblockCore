package net.cengiz1.skyblock.module;

import net.cengiz1.skyblock.SkyblockPlugin;

import java.io.File;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

/**
 * Discovers, loads and manages the lifecycle of module jars placed in
 * {@code plugins/Skyblock/modules/}. Each jar is loaded with its own
 * {@link URLClassLoader} whose parent is the core plugin's class loader, so a
 * module can see Bukkit and every {@code net.cengiz1.skyblock} class while
 * staying isolated from sibling modules.
 */
public final class ModuleManager {

    private final SkyblockPlugin plugin;
    private final File modulesFolder;
    private final Map<String, LoadedModule> modules = new LinkedHashMap<>();

    public ModuleManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
        this.modulesFolder = new File(plugin.getDataFolder(), "modules");
    }

    public void loadModules() {
        if (!modulesFolder.exists() && !modulesFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create modules folder: " + modulesFolder);
            return;
        }

        File[] jars = modulesFolder.listFiles((dir, name) -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            plugin.getLogger().info("No modules found in " + modulesFolder.getName() + "/.");
            return;
        }

        for (File jar : jars)
            loadModule(jar);

        plugin.getLogger().info("Loaded " + modules.size() + " module(s).");
    }

    private void loadModule(File jar) {
        URLClassLoader loader = null;
        try {
            ModuleDescriptor descriptor = readDescriptor(jar);
            if (descriptor == null)
                return;

            if (modules.containsKey(descriptor.getName())) {
                plugin.getLogger().warning("Duplicate module '" + descriptor.getName()
                        + "' (" + jar.getName() + ") ignored.");
                return;
            }

            loader = new URLClassLoader(new java.net.URL[]{jar.toURI().toURL()},
                    plugin.getClass().getClassLoader());

            Class<?> mainClass = Class.forName(descriptor.getMain(), true, loader);
            if (!SkyblockModule.class.isAssignableFrom(mainClass)) {
                plugin.getLogger().warning("Module '" + descriptor.getName() + "' main class "
                        + descriptor.getMain() + " does not implement SkyblockModule.");
                loader.close();
                return;
            }

            SkyblockModule instance = (SkyblockModule) mainClass.getDeclaredConstructor().newInstance();
            File dataFolder = new File(modulesFolder, descriptor.getName());
            ModuleContext context = new ModuleContext(plugin, descriptor.getName(), dataFolder, loader);

            instance.onLoad(context);
            instance.onEnable();

            modules.put(descriptor.getName(), new LoadedModule(descriptor, instance, context, loader));
            plugin.getLogger().info("Enabled module " + descriptor.getName() + " v" + descriptor.getVersion());
        } catch (Throwable error) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load module from " + jar.getName(), error);
            if (loader != null) {
                try {
                    loader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private ModuleDescriptor readDescriptor(File jar) throws Exception {
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry entry = jarFile.getJarEntry("module.yml");
            if (entry == null) {
                plugin.getLogger().warning(jar.getName() + " has no module.yml; skipped.");
                return null;
            }
            try (InputStream in = jarFile.getInputStream(entry)) {
                return ModuleDescriptor.read(in);
            }
        }
    }

    public void unloadModules() {
        List<LoadedModule> ordered = new ArrayList<>(modules.values());
        Collections.reverse(ordered);

        for (LoadedModule module : ordered) {
            String name = module.getDescriptor().getName();
            try {
                module.getInstance().onDisable();
            } catch (Throwable error) {
                plugin.getLogger().log(Level.SEVERE, "Error disabling module " + name, error);
            }
            try {
                module.getContext().teardown();
            } catch (Throwable error) {
                plugin.getLogger().log(Level.WARNING, "Error tearing down module " + name, error);
            }
            try {
                module.getClassLoader().close();
            } catch (Exception ignored) {
            }
        }
        modules.clear();
    }

    /** The enabled module instance for {@code name}, or {@code null}. */
    public SkyblockModule getModule(String name) {
        LoadedModule loaded = modules.get(name);
        return loaded == null ? null : loaded.getInstance();
    }

    public boolean isEnabled(String name) {
        return modules.containsKey(name);
    }

    public Set<String> getModuleNames() {
        return Collections.unmodifiableSet(modules.keySet());
    }
}
