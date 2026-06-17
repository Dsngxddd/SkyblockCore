package net.cengiz1.skyblock.world;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.config.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;

public class WorldManager {

    private final SkyblockPlugin plugin;
    private final SettingsManager settings;

    private World world;
    private World netherWorld;
    private World endWorld;

    public WorldManager(SkyblockPlugin plugin, SettingsManager settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void loadWorld() {
        String name = settings.getWorldName();

        World existing = Bukkit.getWorld(name);
        if (existing != null) {
            this.world = existing;
        } else {
            WorldCreator creator = new WorldCreator(name).environment(World.Environment.NORMAL);
            if (settings.isVoidWorld())
                creator.generator(new VoidGenerator());
            this.world = creator.createWorld();

            if (this.world != null) {
                if (settings.isVoidWorld()) {
                    this.world.setSpawnLocation(0, settings.getIslandHeight() + 5, 0);
                    trySetGameRule(this.world, "doDaylightCycle", false);
                    trySetGameRule(this.world, "doWeatherCycle", false);
                }
                trySetGameRule(this.world, "doMobSpawning", true);
            }
        }

        loadDimensions();
    }

    /**
     * Loads the per-island Nether and End worlds. They share the overworld
     * island's grid coordinates: an island's nether/end plot sits at the same
     * X/Z, just in these dedicated void worlds.
     */
    private void loadDimensions() {
        if (settings.isNetherEnabled())
            this.netherWorld = loadVoidDimension(settings.getNetherWorldName(),
                    World.Environment.NETHER, settings.getNetherIslandHeight());
        if (settings.isEndEnabled())
            this.endWorld = loadVoidDimension(settings.getEndWorldName(),
                    World.Environment.THE_END, settings.getEndIslandHeight());
    }

    private World loadVoidDimension(String name, World.Environment environment, int height) {
        World existing = Bukkit.getWorld(name);
        if (existing != null)
            return existing;

        World created = new WorldCreator(name)
                .environment(environment)
                .generator(new VoidGenerator())
                .createWorld();

        if (created != null) {
            created.setSpawnLocation(0, height + 5, 0);
            trySetGameRule(created, "doMobSpawning", true);
        }
        return created;
    }

    public World getWorld() {
        if (this.world == null)
            this.world = Bukkit.getWorld(settings.getWorldName());
        return this.world;
    }

    public World getNetherWorld() {
        if (this.netherWorld == null && settings.isNetherEnabled())
            this.netherWorld = Bukkit.getWorld(settings.getNetherWorldName());
        return this.netherWorld;
    }

    public World getEndWorld() {
        if (this.endWorld == null && settings.isEndEnabled())
            this.endWorld = Bukkit.getWorld(settings.getEndWorldName());
        return this.endWorld;
    }

    private void trySetGameRule(World world, String rule, boolean value) {
        try {
            GameRule<Boolean> gameRule = (GameRule<Boolean>) GameRule.getByName(rule);
            if (gameRule != null)
                world.setGameRule(gameRule, value);
        } catch (Throwable ignored) {
        }
    }
}
