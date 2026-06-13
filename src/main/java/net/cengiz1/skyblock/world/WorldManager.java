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

    public WorldManager(SkyblockPlugin plugin, SettingsManager settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void loadWorld() {
        String name = settings.getWorldName();

        World existing = Bukkit.getWorld(name);
        if (existing != null) {
            this.world = existing;
            return;
        }

        this.world = new WorldCreator(name)
                .environment(World.Environment.NORMAL)
                .generator(new VoidGenerator())
                .createWorld();

        if (this.world != null) {
            this.world.setSpawnLocation(0, settings.getIslandHeight() + 5, 0);
            trySetGameRule(this.world, "doDaylightCycle", false);
            trySetGameRule(this.world, "doWeatherCycle", false);
            trySetGameRule(this.world, "doMobSpawning", true);
        }
    }

    public World getWorld() {
        if (this.world == null)
            this.world = Bukkit.getWorld(settings.getWorldName());
        return this.world;
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
