package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandPermission;
import net.cengiz1.skyblock.util.SafeLocation;
import org.bukkit.entity.Player;

public class WarpCommands extends CommandHandler {

    public WarpCommands(SkyblockPlugin plugin) {
        super(plugin);
    }

    public void warp(Player player, String targetName, String warpName) {
        if (targetName == null) {
            openWarpMenu(player);
            return;
        }
        plugin.getWarpService().warpByName(player, targetName, sanitize(warpName));
    }

    public void openWarpMenu(Player player) {
        if (plugin.getMenuManager().has("warp"))
            plugin.getMenuManager().open(player, "warp", null);
        else
            plugin.getMessages().send(player, "unknown-subcommand");
    }

    public void setWarp(Player player, String name) {
        Island island = requirePermission(player, IslandPermission.SET_WARP);
        if (island == null)
            return;
        if (plugin.getIslandManager().getIslandAt(player.getLocation()) != island) {
            plugin.getMessages().send(player, "must-be-on-island");
            return;
        }
        if (!SafeLocation.isSafe(player.getLocation())) {
            plugin.getMessages().send(player, "warp-set-unsafe");
            return;
        }

        String warpName = sanitize(name);
        if (warpName == null)
            warpName = "ada";

        if (!island.hasWarp(warpName)) {
            int limit = plugin.getWarpService().getWarpLimit(player);
            if (island.getWarpCount() >= limit) {
                plugin.getMessages().send(player, "warp-limit-reached", "{limit}", String.valueOf(limit));
                return;
            }
        }

        island.setWarp(warpName, player.getLocation());
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "warp-set-named", "{name}", warpName);
    }

    public void delWarp(Player player, String name) {
        Island island = requirePermission(player, IslandPermission.SET_WARP);
        if (island == null)
            return;
        if (!island.hasWarp()) {
            plugin.getMessages().send(player, "warp-none");
            return;
        }

        String warpName = sanitize(name);
        if (warpName == null)
            warpName = island.getDefaultWarpName();
        if (warpName == null || !island.removeWarp(warpName)) {
            plugin.getMessages().send(player, "warp-unknown-name", "{name}", name == null ? "-" : name);
            return;
        }
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "warp-removed");
    }

    private String sanitize(String name) {
        if (name == null)
            return null;
        String cleaned = name.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return cleaned.isEmpty() ? null : cleaned;
    }
}
