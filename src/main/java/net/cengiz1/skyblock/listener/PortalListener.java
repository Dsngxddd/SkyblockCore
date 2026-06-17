package net.cengiz1.skyblock.listener;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandManager;
import net.cengiz1.skyblock.world.DimensionManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;


public class PortalListener implements Listener {

    private final IslandManager islandManager;
    private final DimensionManager dimensions;

    public PortalListener(SkyblockPlugin plugin) {
        this.islandManager = plugin.getIslandManager();
        this.dimensions = plugin.getDimensionManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        Location from = event.getFrom();
        World fromWorld = from.getWorld();
        if (!dimensions.isIslandWorld(fromWorld))
            return;

        TeleportCause cause = event.getCause();
        if (cause != TeleportCause.NETHER_PORTAL && cause != TeleportCause.END_PORTAL)
            return;

        Island island = islandManager.getGoverningIsland(from);
        if (island == null) {
            event.setCancelled(true);
            return;
        }

        Location target;
        if (cause == TeleportCause.NETHER_PORTAL) {
            target = dimensions.isNetherWorld(fromWorld)
                    ? dimensions.overworldHome(island)
                    : dimensions.prepareNether(island);
        } else {
            target = dimensions.isEndWorld(fromWorld)
                    ? dimensions.overworldHome(island)
                    : dimensions.prepareEnd(island);
        }

        if (target == null) {
            event.setCancelled(true);
            return;
        }

        event.setCanCreatePortal(false);
        event.setTo(target);
    }
}
