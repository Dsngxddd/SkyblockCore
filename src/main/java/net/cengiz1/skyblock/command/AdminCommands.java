package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.proxy.ProxyManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class AdminCommands extends CommandHandler {

    public AdminCommands(SkyblockPlugin plugin) {
        super(plugin);
    }

    public void proxyStatus(Player player) {
        if (!player.hasPermission("skyblock.admin")) {
            plugin.getMessages().send(player, "no-permission");
            return;
        }
        ProxyManager proxy = plugin.getProxyManager();
        boolean enabled = proxy != null && proxy.isEnabled();
        String server = enabled ? proxy.getServerName() : "-";
        String createServer = "-";
        if (enabled)
            createServer = proxy.isLocalCreate()
                    ? plugin.getMessages().get("proxy-create-local")
                    : String.join(", ", proxy.getCreateServers());

        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        String islandServer;
        if (island == null)
            islandServer = plugin.getMessages().get("proxy-island-none");
        else if (island.getServerName() == null)
            islandServer = plugin.getMessages().get("proxy-island-unassigned");
        else
            islandServer = island.getServerName();

        String state = enabled
                ? plugin.getMessages().get("proxy-state-on")
                : plugin.getMessages().get("proxy-state-off");

        plugin.getMessages().send(player, "proxy-status",
                "{state}", state,
                "{server}", server,
                "{create}", createServer,
                "{island}", islandServer);
    }

    public void setGlobalSpawn(Player player) {
        if (!player.hasPermission("skyblock.admin")) {
            plugin.getMessages().send(player, "no-permission");
            return;
        }
        Location loc = player.getLocation();
        plugin.getConfig().set("spawn.world", loc.getWorld().getName());
        plugin.getConfig().set("spawn.x", loc.getX());
        plugin.getConfig().set("spawn.y", loc.getY());
        plugin.getConfig().set("spawn.z", loc.getZ());
        plugin.getConfig().set("spawn.yaw", (double) loc.getYaw());
        plugin.getConfig().set("spawn.pitch", (double) loc.getPitch());
        plugin.saveConfig();
        plugin.getSettings().reload();
        plugin.getMessages().send(player, "global-spawn-set");
    }
}
