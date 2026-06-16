package net.cengiz1.skyblock.upgrade;

import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandManager;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class UpgradeEffectListener implements Listener {

    private final IslandManager islandManager;
    private final UpgradeManager upgrades;

    public UpgradeEffectListener(IslandManager islandManager, UpgradeManager upgrades) {
        this.islandManager = islandManager;
        this.upgrades = upgrades;
    }

    @EventHandler(ignoreCancelled = true)
    public void onForm(BlockFormEvent event) {
        Material formed = event.getNewState().getType();
        if (formed != Material.COBBLESTONE && formed != Material.STONE
                && formed != Material.BASALT && formed != Material.OBSIDIAN)
            return;

        Island island = this.islandManager.getIslandAt(event.getBlock().getLocation());
        if (island == null)
            return;

        Material picked = this.upgrades.pickGeneratorBlock(island, "generator", formed);
        if (picked != formed)
            event.getNewState().setType(picked);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent event) {
        BlockData data = event.getBlock().getBlockData();
        if (!(data instanceof Ageable))
            return;
        Ageable ageable = (Ageable) data;
        if (ageable.getAge() < ageable.getMaximumAge())
            return;

        Island island = this.islandManager.getIslandAt(event.getBlock().getLocation());
        if (island == null)
            return;

        double chance = this.upgrades.getValue(island, "crop-growth", 0);
        if (chance <= 0)
            return;
        if (ThreadLocalRandom.current().nextDouble(100.0) >= chance)
            return;

        for (ItemStack drop : event.getBlock().getDrops(event.getPlayer().getInventory().getItemInMainHand())) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop.clone());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Island island = this.islandManager.getIslandAt(event.getEntity().getLocation());
        if (island == null)
            return;

        int multiplier = (int) this.upgrades.getValue(island, "mob-drops", 1);
        if (multiplier <= 1)
            return;

        List<ItemStack> extra = new ArrayList<>();
        for (ItemStack drop : event.getDrops())
            for (int i = 1; i < multiplier; i++)
                extra.add(drop.clone());
        event.getDrops().addAll(extra);

        event.setDroppedExp(event.getDroppedExp() * multiplier);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        if (!(event.getSpawner() instanceof CreatureSpawner))
            return;

        Island island = this.islandManager.getIslandAt(event.getLocation());
        if (island == null)
            return;

        double factor = this.upgrades.getValue(island, "spawner-rates", 1.0);
        if (factor >= 1.0 || factor <= 0)
            return;

        CreatureSpawner spawner = event.getSpawner();
        try {
            spawner.setDelay((int) Math.max(1, spawner.getDelay() * factor));
            spawner.update(true, false);
        } catch (Throwable ignored) {
        }
    }
}
