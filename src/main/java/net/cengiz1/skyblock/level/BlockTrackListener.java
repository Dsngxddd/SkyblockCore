package net.cengiz1.skyblock.level;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockTrackListener implements Listener {

    private final SkyblockPlugin plugin;
    private final IslandManager islandManager;
    private final BlockValueManager blockValues;
    private final LevelManager levels;

    public BlockTrackListener(SkyblockPlugin plugin, IslandManager islandManager,
                              BlockValueManager blockValues, LevelManager levels) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.blockValues = blockValues;
        this.levels = levels;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        applyChange(event.getBlock().getType(), event.getBlock().getLocation(), true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        applyChange(event.getBlock().getType(), event.getBlock().getLocation(), false);
    }

    private void applyChange(Material material, org.bukkit.Location location, boolean place) {
        double value = this.blockValues.getValue(material);
        if (value == 0.0)
            return;

        Island island = this.islandManager.getIslandAt(location);
        if (island == null)
            return;

        int oldLevel = island.getLevel();
        island.addPoints(place ? value : -value);

        int newLevel = this.levels.levelFromPoints(island.getPoints());
        if (newLevel != oldLevel) {
            island.setLevel(newLevel);
            if (newLevel > oldLevel) {
                this.islandManager.messageMembers(island, "level-up",
                        "{level}", String.valueOf(newLevel));
            }
        }

        this.islandManager.saveAsync(island);
    }
}
