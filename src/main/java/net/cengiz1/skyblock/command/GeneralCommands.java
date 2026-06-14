package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandManager;
import net.cengiz1.skyblock.island.IslandRole;
import net.cengiz1.skyblock.proxy.ProxyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class GeneralCommands extends CommandHandler {

    public GeneralCommands(SkyblockPlugin plugin) {
        super(plugin);
    }

    public void openMenu(Player player, String menuId) {
        if (plugin.getMenuManager().has(menuId))
            plugin.getMenuManager().open(player, menuId, null);
        else
            plugin.getMessages().send(player, "unknown-subcommand");
    }

    public void home(Player player) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        ProxyManager proxy = plugin.getProxyManager();
        if (proxy != null && proxy.handleTeleport(player, island))
            return;
        plugin.getMessages().send(player, "teleporting");
        plugin.getIslandManager().teleportHome(player, island);
    }

    public void visit(Player player, String targetName) {
        if (targetName == null) {
            plugin.getMessages().send(player, "usage-visit");
            return;
        }
        plugin.getVisitService().visitByName(player, targetName);
    }

    public void create(Player player, String type) {
        IslandManager manager = plugin.getIslandManager();
        if (manager.getByMember(player.getUniqueId()) != null) {
            plugin.getMessages().send(player, "already-have-island");
            return;
        }
        if (manager.getCreationService().isCreating(player.getUniqueId())) {
            plugin.getMessages().send(player, "creating-in-progress");
            return;
        }
        if (type != null && manager.getSchematicService().isReady() && !manager.getSchematicService().has(type)) {
            plugin.getMessages().send(player, "invalid-schematic", "{types}", joinTypes());
            return;
        }

        ProxyManager proxy = plugin.getProxyManager();
        if (proxy != null && proxy.handleRemoteCreate(player, type))
            return;

        plugin.getMessages().send(player, "creating");
        manager.getCreationService().create(player, type).whenComplete((result, error) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    switch (result) {
                        case SUCCESS: plugin.getMessages().send(player, "created"); break;
                        case ALREADY_HAS_ISLAND: plugin.getMessages().send(player, "already-have-island"); break;
                        case ALREADY_CREATING: plugin.getMessages().send(player, "creating-in-progress"); break;
                        case INVALID_SCHEMATIC: plugin.getMessages().send(player, "invalid-schematic", "{types}", joinTypes()); break;
                        default: plugin.getMessages().send(player, "create-failed"); break;
                    }
                }));
    }

    public void delete(Player player) {
        Island island = plugin.getIslandManager().getByOwner(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        if (plugin.getMenuManager().has("delete-confirm"))
            plugin.getMenuManager().open(player, "delete-confirm", island.getUniqueId());
        else
            plugin.getIslandManager().deleteIslandConfirmed(player, island);
    }

    public void level(Player player) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        double next = plugin.getLevelManager().pointsForNextLevel(island.getLevel());
        plugin.getMessages().send(player, "level-info",
                "{level}", String.valueOf(island.getLevel()),
                "{points}", formatNumber(island.getPoints()),
                "{next}", next < 0 ? "MAX" : formatNumber(next));
    }

    public void info(Player player) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        plugin.getMessages().send(player, "info-header");
        plugin.getMessages().send(player, "info-owner", "{player}", nameOf(island.getOwner()));
        plugin.getMessages().send(player, "info-level",
                "{level}", String.valueOf(island.getLevel()),
                "{points}", formatNumber(island.getPoints()));
        int limit = (int) plugin.getUpgradeManager().getValue(island, "team-limit", 4);
        plugin.getMessages().send(player, "info-members",
                "{count}", String.valueOf(island.getMemberCount()),
                "{limit}", String.valueOf(limit));
    }

    public void members(Player player) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        plugin.getMessages().send(player, "members-header");
        plugin.getMessages().send(player, "members-entry",
                "{player}", nameOf(island.getOwner()), "{role}", IslandRole.OWNER.getDisplayName());
        for (Map.Entry<UUID, IslandRole> entry : island.getMembers().entrySet())
            plugin.getMessages().send(player, "members-entry",
                    "{player}", nameOf(entry.getKey()), "{role}", entry.getValue().getDisplayName());
    }
}
