package net.cengiz1.skyblock.world;

import net.cengiz1.skyblock.config.SettingsManager;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.schematic.SchematicService;
import org.bukkit.Location;
import org.bukkit.World;

public class DimensionManager {

    private final SettingsManager settings;
    private final WorldManager worldManager;
    private final SchematicService schematicService;

    public DimensionManager(SettingsManager settings, WorldManager worldManager, SchematicService schematicService) {
        this.settings = settings;
        this.worldManager = worldManager;
        this.schematicService = schematicService;
    }

    public boolean isIslandWorld(World world) {
        if (world == null)
            return false;
        String name = world.getName();
        return name.equals(settings.getWorldName())
                || name.equals(settings.getNetherWorldName())
                || name.equals(settings.getEndWorldName());
    }

    public boolean isNetherWorld(World world) {
        return world != null && world.getName().equals(settings.getNetherWorldName());
    }

    public boolean isEndWorld(World world) {
        return world != null && world.getName().equals(settings.getEndWorldName());
    }

    public Location prepareNether(Island island) {
        World world = worldManager.getNetherWorld();
        if (world == null)
            return null;
        int cx = island.getCenterX();
        int cz = island.getCenterZ();
        int cy = settings.getNetherIslandHeight();
        world.getChunkAt(cx >> 4, cz >> 4).load(true);

        String file = settings.getNetherSchematic();
        boolean useSchematic = canUseSchematic(file);

        Location home = useSchematic
                ? new Location(world, cx + settings.getNetherHomeX(), cy + settings.getNetherHomeY(),
                        cz + settings.getNetherHomeZ(), settings.getNetherHomeYaw(), 0f)
                : new Location(world, cx + 0.5, cy + 1, cz + 0.5, 0f, 0f);

        if (!isBuilt(world, home, useSchematic, cx, cy, cz)) {
            if (!(useSchematic && schematicService.pasteFile(world, cx, cy, cz, file)))
                DimensionBuilder.buildNether(world, cx, cy, cz);
        }
        return home;
    }

    public Location prepareEnd(Island island) {
        World world = worldManager.getEndWorld();
        if (world == null)
            return null;
        int cx = island.getCenterX();
        int cz = island.getCenterZ();
        int cy = settings.getEndIslandHeight();
        world.getChunkAt(cx >> 4, cz >> 4).load(true);

        String file = settings.getEndSchematic();
        boolean useSchematic = canUseSchematic(file);

        Location home = useSchematic
                ? new Location(world, cx + settings.getEndHomeX(), cy + settings.getEndHomeY(),
                        cz + settings.getEndHomeZ(), settings.getEndHomeYaw(), 0f)
                : new Location(world, cx + 0.5, cy + 1, cz + 3.5, 180f, 0f);

        if (!isBuilt(world, home, useSchematic, cx, cy, cz)) {
            if (!(useSchematic && schematicService.pasteFile(world, cx, cy, cz, file)))
                DimensionBuilder.buildEnd(world, cx, cy, cz);
        }
        return home;
    }

    public Location overworldHome(Island island) {
        World world = worldManager.getWorld();
        if (world == null)
            return null;
        Location home = island.getHome(world);
        home.getChunk().load(true);
        return home;
    }

    private boolean canUseSchematic(String file) {
        return schematicService != null
                && file != null
                && !file.isBlank()
                && schematicService.isAvailable()
                && schematicService.hasFile(file);
    }

    private boolean isBuilt(World world, Location home, boolean useSchematic, int cx, int cy, int cz) {
        if (useSchematic)
            return !world.getBlockAt(home.getBlockX(), home.getBlockY() - 1, home.getBlockZ()).getType().isAir();
        return !world.getBlockAt(cx, cy, cz).getType().isAir();
    }
}
