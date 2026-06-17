package net.cengiz1.skyblock.world;

import net.cengiz1.skyblock.config.SettingsManager;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.schematic.SchematicService;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Resolves and lazily builds an island's per-dimension plots. The plots live at
 * the same grid coordinates as the overworld island, in the dedicated void
 * worlds, on the same backend server the island belongs to — so this needs no
 * extra cross-server state.
 *
 * Each dimension's starter plot can be supplied as a WorldEdit schematic file
 * (configured under {@code nether.schematic} / {@code end.schematic}); when no
 * file is configured (or FastAsyncWorldEdit is missing) it falls back to the
 * built-in {@link DimensionBuilder} that lays the plot out block-by-block.
 */
public class DimensionManager {

    private final SettingsManager settings;
    private final WorldManager worldManager;
    private SchematicService schematicService;

    public DimensionManager(SettingsManager settings, WorldManager worldManager) {
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

    /** Builds the nether plot if it isn't there yet and returns its safe home. */
    public Location prepareNether(Island island) {
        World world = worldManager.getNetherWorld();
        if (world == null)
            return null;
        int cx = island.getCenterX();
        int cz = island.getCenterZ();
        int cy = settings.getNetherIslandHeight();
        world.getChunkAt(cx >> 4, cz >> 4).load(true);
        if (world.getBlockAt(cx, cy, cz).getType().isAir())
            DimensionBuilder.buildNether(world, cx, cy, cz);
        return new Location(world, cx + 0.5, cy + 1, cz + 0.5, 0f, 0f);
    }

    /** Builds the end plot if it isn't there yet and returns its safe home. */
    public Location prepareEnd(Island island) {
        World world = worldManager.getEndWorld();
        if (world == null)
            return null;
        int cx = island.getCenterX();
        int cz = island.getCenterZ();
        int cy = settings.getEndIslandHeight();
        world.getChunkAt(cx >> 4, cz >> 4).load(true);
        if (world.getBlockAt(cx, cy, cz).getType().isAir())
            DimensionBuilder.buildEnd(world, cx, cy, cz);
        return new Location(world, cx + 0.5, cy + 1, cz + 3.5, 180f, 0f);
    }

    /** The island's overworld home (where players return to from a portal). */
    public Location overworldHome(Island island) {
        World world = worldManager.getWorld();
        if (world == null)
            return null;
        Location home = island.getHome(world);
        home.getChunk().load(true);
        return home;
    }
}
