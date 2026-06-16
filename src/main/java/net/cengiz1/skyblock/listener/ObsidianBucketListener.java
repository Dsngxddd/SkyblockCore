package net.cengiz1.skyblock.listener;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandManager;
import net.cengiz1.skyblock.island.IslandPermission;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Right-click an obsidian block with an empty bucket to break it and get the
 * lava back as a lava bucket, so lava is never wasted when running an obsidian
 * generator. On an island the player must be allowed to break blocks.
 */
public class ObsidianBucketListener implements Listener {

    private final SkyblockPlugin plugin;
    private final IslandManager islandManager;

    public ObsidianBucketListener(SkyblockPlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND)
            return;
        if (!plugin.getSettings().isObsidianBucketToLava())
            return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.OBSIDIAN)
            return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.BUCKET)
            return;
        Island island = islandManager.getIslandAt(block.getLocation());
        if (island != null && !island.hasPermission(player.getUniqueId(), IslandPermission.BLOCK_BREAK))
            return;

        event.setCancelled(true);
        block.setType(Material.AIR);

        ItemStack lavaBucket = new ItemStack(Material.LAVA_BUCKET);
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(lavaBucket);
        } else {
            hand.setAmount(hand.getAmount() - 1);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(lavaBucket);
            for (ItemStack leftover : overflow.values())
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL_LAVA, 1.0f, 1.0f);
    }
}
