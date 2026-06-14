package net.cengiz1.skyblock.listener;

import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandFlag;
import net.cengiz1.skyblock.island.IslandManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class PvpListener implements Listener {

    private final IslandManager islandManager;

    public PvpListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        if (resolveAttacker(event) == null)
            return;

        Island island = islandManager.getIslandAt(event.getEntity().getLocation());
        if (island != null && !island.getFlag(IslandFlag.PVP))
            event.setCancelled(true);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player)
            return (Player) event.getDamager();
        if (event.getDamager() instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) event.getDamager()).getShooter();
            if (shooter instanceof Player)
                return (Player) shooter;
        }
        return null;
    }
}
