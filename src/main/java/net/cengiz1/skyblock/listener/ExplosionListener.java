package net.cengiz1.skyblock.listener;

import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandFlag;
import net.cengiz1.skyblock.island.IslandManager;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ExplosionListener implements Listener {

    private final IslandManager islandManager;

    public ExplosionListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        Island island = islandManager.getIslandAt(event.getLocation());
        if (island == null)
            return;

        boolean creeper = event.getEntity() instanceof Creeper;
        boolean tnt = event.getEntity() instanceof TNTPrimed;

        if ((creeper && !island.getFlag(IslandFlag.CREEPER_EXPLOSION))
                || (tnt && !island.getFlag(IslandFlag.TNT_EXPLOSION)))
            event.blockList().clear();
    }
}
