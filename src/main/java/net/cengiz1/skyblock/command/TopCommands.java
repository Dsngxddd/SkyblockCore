package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /island top — prints the island leaderboard in chat.
 * /island top holo — toggles a personal floating leaderboard hologram that
 * follows the player around.
 */
public class TopCommands extends CommandHandler {

    public TopCommands(SkyblockPlugin plugin) {
        super(plugin);
    }

    public void handle(Player player, String arg) {
        if (arg != null) {
            String a = arg.toLowerCase(java.util.Locale.ROOT);
            if (a.equals("holo") || a.equals("hologram") || a.equals("takip")) {
                if (plugin.getTopHologramManager() == null) {
                    plugin.getMessages().send(player, "unknown-subcommand");
                    return;
                }
                boolean shown = plugin.getTopHologramManager().toggle(player);
                plugin.getMessages().send(player, shown ? "top-holo-on" : "top-holo-off");
                return;
            }
        }

        int lines = Math.max(1, plugin.getConfig().getInt("top.chat-lines", 10));
        List<Island> top = plugin.getTopService().getTop(lines);
        if (top.isEmpty()) {
            plugin.getMessages().send(player, "top-empty");
            return;
        }
        plugin.getMessages().send(player, "top-header");
        int rank = 1;
        for (Island island : top) {
            plugin.getMessages().send(player, "top-entry",
                    "{rank}", String.valueOf(rank++),
                    "{owner}", nameOf(island.getOwner()),
                    "{level}", String.valueOf(island.getLevel()),
                    "{points}", formatNumber(island.getPoints()));
        }
    }
}
