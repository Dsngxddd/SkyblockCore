package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.RoleData;
import net.cengiz1.skyblock.island.RoleManager;
import net.cengiz1.skyblock.proxy.ProxyManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class AdminCommands extends CommandHandler {

    public static final String PERMISSION = "skyblock.admin";

    public AdminCommands(SkyblockPlugin plugin) {
        super(plugin);
    }

    /** Names of the /is admin sub-commands, used for tab completion. */
    public static final String[] SUBCOMMANDS = {
            "info", "tp", "setowner", "delete", "bank", "role", "points", "level", "reload", "help"
    };

    public boolean hasAccess(Player player) {
        return player.hasPermission(PERMISSION);
    }

    public void admin(Player player, String[] args) {
        if (!hasAccess(player)) {
            plugin.getMessages().send(player, "unknown-subcommand");
            return;
        }
        String sub = args.length >= 2 ? args[1].toLowerCase() : "help";
        switch (sub) {
            case "info":     adminInfo(player, arg(args, 2)); break;
            case "tp":       adminTp(player, arg(args, 2)); break;
            case "setowner": adminSetOwner(player, arg(args, 2), arg(args, 3)); break;
            case "delete":   adminDelete(player, arg(args, 2)); break;
            case "bank":     adminBank(player, arg(args, 2), arg(args, 3), arg(args, 4)); break;
            case "role":     adminRole(player, arg(args, 2), arg(args, 3), arg(args, 4)); break;
            case "points":   adminPoints(player, arg(args, 2), arg(args, 3), arg(args, 4)); break;
            case "level":    adminLevel(player, arg(args, 2), arg(args, 3)); break;
            case "reload":   adminReload(player); break;
            default:         adminHelp(player); break;
        }
    }

    private void adminHelp(Player player) {
        msg(player, "&c&lSkyblock Admin");
        msg(player, "&7/is admin info <player> &8- island details");
        msg(player, "&7/is admin tp <player> &8- teleport to their island");
        msg(player, "&7/is admin setowner <player> <newOwner> &8- transfer the island");
        msg(player, "&7/is admin delete <player> &8- delete the island");
        msg(player, "&7/is admin bank <player> <set|add|remove> <amount>");
        msg(player, "&7/is admin role <player> <member> <role> &8- set a member's role");
        msg(player, "&7/is admin points <player> <set|add> <amount>");
        msg(player, "&7/is admin level <player> <level>");
        msg(player, "&7/is admin reload &8- reload configs");
    }

    private void adminInfo(Player player, String name) {
        Island island = target(player, name);
        if (island == null)
            return;
        msg(player, "&c&lIsland of &f" + nameOf(island.getOwner()));
        msg(player, "&7Owner: &f" + nameOf(island.getOwner()) + " &8| &7Server: &f"
                + (island.getServerName() == null ? "-" : island.getServerName()));
        msg(player, "&7Level: &f" + island.getLevel() + " &8| &7Points: &f" + formatNumber(island.getPoints())
                + " &8| &7Bank: &f" + formatNumber(island.getBank()));
        msg(player, "&7Members: &f" + island.getMemberCount() + " &8| &7Warps: &f" + island.getWarpCount()
                + " &8| &7Custom roles: &f" + island.getCustomRoles().size());
    }

    private void adminTp(Player player, String name) {
        Island island = target(player, name);
        if (island == null)
            return;
        plugin.getIslandManager().teleportHome(player, island);
        msg(player, "&aTeleported to &f" + nameOf(island.getOwner()) + "&a's island.");
    }

    private void adminSetOwner(Player player, String name, String newOwnerName) {
        if (name == null || newOwnerName == null) {
            msg(player, "&cUsage: /is admin setowner <player> <newOwner>");
            return;
        }
        Island island = target(player, name);
        if (island == null)
            return;
        OfflinePlayer newOwner = resolveOffline(newOwnerName);
        if (newOwner == null) {
            msg(player, "&cPlayer not found: " + newOwnerName);
            return;
        }
        if (island.isOwner(newOwner.getUniqueId())) {
            msg(player, "&cThat player already owns this island.");
            return;
        }
        Island theirs = plugin.getIslandManager().getByOwner(newOwner.getUniqueId());
        if (theirs != null) {
            msg(player, "&c" + newOwnerName + " already owns another island.");
            return;
        }
        plugin.getIslandManager().transferOwnership(island, newOwner.getUniqueId());
        msg(player, "&aIsland ownership transferred to &f" + newOwnerName + "&a.");
    }

    private void adminDelete(Player player, String name) {
        Island island = target(player, name);
        if (island == null)
            return;
        plugin.getIslandManager().deleteIsland(island);
        msg(player, "&aDeleted &f" + nameOf(island.getOwner()) + "&a's island.");
    }

    private void adminBank(Player player, String name, String op, String amountArg) {
        Island island = target(player, name);
        if (island == null)
            return;
        if (op == null || amountArg == null) {
            msg(player, "&cUsage: /is admin bank <player> <set|add|remove> <amount>");
            return;
        }
        double amount = parseAmount(amountArg);
        if (amount < 0) {
            msg(player, "&cInvalid amount.");
            return;
        }
        switch (op.toLowerCase()) {
            case "set":    island.setBank(amount); break;
            case "add":    island.depositBank(amount); break;
            case "remove":
            case "take":   island.withdrawBank(amount); break;
            default:       msg(player, "&cUse set, add or remove."); return;
        }
        plugin.getIslandManager().saveAsync(island);
        msg(player, "&aBank of &f" + nameOf(island.getOwner()) + "&a is now &e" + formatNumber(island.getBank()) + "&a.");
    }

    private void adminRole(Player player, String name, String memberName, String roleId) {
        Island island = target(player, name);
        if (island == null)
            return;
        if (memberName == null || roleId == null) {
            msg(player, "&cUsage: /is admin role <player> <member> <role>");
            return;
        }
        OfflinePlayer member = resolveOffline(memberName);
        if (member == null || !island.isMember(member.getUniqueId())) {
            msg(player, "&c" + memberName + " is not a member of that island.");
            return;
        }
        if (island.isOwner(member.getUniqueId())) {
            msg(player, "&cThat player is the owner; use setowner instead.");
            return;
        }
        RoleData role = island.resolveRoleById(roleId);
        if (role == null || role.getId().equals(RoleManager.OWNER_ID) || role.getId().equals(RoleManager.VISITOR_ID)) {
            msg(player, "&cUnknown role: " + roleId);
            return;
        }
        island.setRole(member.getUniqueId(), role.getId());
        plugin.getIslandManager().saveAsync(island);
        msg(player, "&aSet &f" + memberName + "&a's role to &f" + role.getDisplayName() + "&a.");
    }

    private void adminPoints(Player player, String name, String op, String amountArg) {
        Island island = target(player, name);
        if (island == null)
            return;
        if (op == null || amountArg == null) {
            msg(player, "&cUsage: /is admin points <player> <set|add> <amount>");
            return;
        }
        double amount = parseAmount(amountArg);
        if (amount < 0) {
            msg(player, "&cInvalid amount.");
            return;
        }
        if (op.equalsIgnoreCase("add"))
            island.addPoints(amount);
        else if (op.equalsIgnoreCase("set"))
            island.setPoints(amount);
        else {
            msg(player, "&cUse set or add.");
            return;
        }
        island.setLevel(plugin.getLevelManager().levelFromPoints(island.getPoints()));
        plugin.getIslandManager().saveAsync(island);
        msg(player, "&aPoints of &f" + nameOf(island.getOwner()) + "&a set to &e" + formatNumber(island.getPoints())
                + " &7(level " + island.getLevel() + ")&a.");
    }

    private void adminLevel(Player player, String name, String levelArg) {
        Island island = target(player, name);
        if (island == null)
            return;
        if (levelArg == null) {
            msg(player, "&cUsage: /is admin level <player> <level>");
            return;
        }
        try {
            island.setLevel(Integer.parseInt(levelArg.trim()));
        } catch (NumberFormatException error) {
            msg(player, "&cInvalid level.");
            return;
        }
        plugin.getIslandManager().saveAsync(island);
        msg(player, "&aLevel of &f" + nameOf(island.getOwner()) + "&a set to &e" + island.getLevel() + "&a.");
    }

    private void adminReload(Player player) {
        plugin.reloadAll();
        msg(player, "&aSkyblock configuration reloaded.");
    }

    private Island target(Player admin, String name) {
        if (name == null) {
            msg(admin, "&cYou must specify a player.");
            return null;
        }
        OfflinePlayer offline = resolveOffline(name);
        if (offline == null) {
            msg(admin, "&cPlayer not found: " + name);
            return null;
        }
        UUID id = offline.getUniqueId();
        Island island = plugin.getIslandManager().getByOwner(id);
        if (island == null)
            island = plugin.getIslandManager().getByMember(id);
        if (island == null) {
            msg(admin, "&c" + name + " does not have an island.");
            return null;
        }
        return island;
    }

    private double parseAmount(String arg) {
        if (arg == null)
            return -1;
        try {
            return Double.parseDouble(arg.trim().replace(",", "."));
        } catch (NumberFormatException error) {
            return -1;
        }
    }

    private String arg(String[] args, int index) {
        return args.length > index ? args[index] : null;
    }

    private void msg(Player player, String text) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&cAdmin&8] " + text));
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
