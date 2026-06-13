package net.cengiz1.skyblock.listener;

import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandFlag;
import net.cengiz1.skyblock.island.IslandManager;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Material;
import org.bukkit.projectiles.ProjectileSource;

public class IslandFlagListener implements Listener {

    private final IslandManager islandManager;

    public IslandFlagListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Player attacker = resolveAttacker(event);
        if (attacker == null)
            return;

        Island island = islandManager.getIslandAt(event.getEntity().getLocation());
        if (island != null && !island.getFlag(IslandFlag.PVP))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM
                || reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)
            return;

        if (!(event.getEntity() instanceof Monster))
            return;

        Island island = islandManager.getIslandAt(event.getLocation());
        if (island != null && !island.getFlag(IslandFlag.MOB_SPAWNING))
            event.setCancelled(true);
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

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null)
            return;

        Island island = islandManager.getIslandAt(event.getClickedBlock().getLocation());
        if (island == null)
            return;

        if (!island.getOwner().equals(event.getPlayer().getUniqueId())
                && !island.getFlag(IslandFlag.VISITOR_INTERACT))
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