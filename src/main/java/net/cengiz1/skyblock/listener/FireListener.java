package net.cengiz1.skyblock.listener;

import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandFlag;
import net.cengiz1.skyblock.island.IslandManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockSpreadEvent;

public class FireListener implements Listener {

    private final IslandManager islandManager;

    public FireListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        Island island = islandManager.getIslandAt(event.getBlock().getLocation());
        if (island != null && !island.getFlag(IslandFlag.FIRE_SPREAD))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() != Material.FIRE)
            return;

        Island island = islandManager.getIslandAt(event.getBlock().getLocation());
        if (island != null && !island.getFlag(IslandFlag.FIRE_SPREAD))
            event.setCancelled(true);
    }
}
