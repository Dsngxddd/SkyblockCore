package net.cengiz1.skyblock.listener;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandPermission;
import net.cengiz1.skyblock.util.SafeLocation;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class WarpSignListener implements Listener {

    private final SkyblockPlugin plugin;

    public WarpSignListener(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        String tag = plugin.getConfig().getString("warp.sign-tag", "[warp]");
        String first = ChatColor.stripColor(event.getLine(0));
        if (first == null || !first.trim().equalsIgnoreCase(tag))
            return;

        Player player = event.getPlayer();
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null || plugin.getIslandManager().getIslandAt(player.getLocation()) != island) {
            plugin.getMessages().send(player, "must-be-on-island");
            event.setLine(0, "");
            return;
        }
        if (!island.hasPermission(player.getUniqueId(), IslandPermission.SET_WARP)) {
            plugin.getMessages().send(player, "no-island-permission");
            event.setLine(0, "");
            return;
        }
        if (!SafeLocation.isSafe(player.getLocation())) {
            plugin.getMessages().send(player, "warp-set-unsafe");
            event.setLine(0, "");
            return;
        }


        String warpName = sanitize(ChatColor.stripColor(event.getLine(1)));
        if (warpName == null)
            warpName = "ada";

        if (!island.hasWarp(warpName)) {
            int limit = plugin.getWarpService().getWarpLimit(player);
            if (island.getWarpCount() >= limit) {
                plugin.getMessages().send(player, "warp-limit-reached", "{limit}", String.valueOf(limit));
                event.setLine(0, "");
                return;
            }
        }

        island.setWarp(warpName, player.getLocation());
        plugin.getIslandManager().saveAsync(island);

        String display = plugin.getConfig().getString("warp.sign-display", "&9[Warp]");
        event.setLine(0, ChatColor.translateAlternateColorCodes('&', display));
        event.setLine(1, name(player));
        event.setLine(2, ChatColor.translateAlternateColorCodes('&', "&7" + warpName));
        event.setLine(3, "");
        plugin.getMessages().send(player, "warp-set");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null)
            return;
        Block block = event.getClickedBlock();
        BlockState state = block.getState();
        if (!(state instanceof Sign))
            return;

        Sign sign = (Sign) state;
        String display = ChatColor.stripColor(
                ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("warp.sign-display", "&9[Warp]")));
        String first = ChatColor.stripColor(sign.getLine(0));
        if (first == null || !first.equalsIgnoreCase(display))
            return;

        plugin.getWarpService().openWarpMenuOrWarp(event.getPlayer(), block.getLocation());
    }

    private String name(Player player) {
        return player.getName();
    }

    private String sanitize(String name) {
        if (name == null)
            return null;
        String cleaned = name.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "");
        return cleaned.isEmpty() ? null : cleaned;
    }
}
