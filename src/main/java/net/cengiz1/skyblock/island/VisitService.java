package net.cengiz1.skyblock.island;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.proxy.ProxyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class VisitService {

    private final SkyblockPlugin plugin;

    public VisitService(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void visitByName(Player viewer, String targetName) {
        OfflinePlayer target = resolveOffline(targetName);
        if (target == null) {
            plugin.getMessages().send(viewer, "player-not-found", "{player}", targetName);
            return;
        }
        Island island = plugin.getIslandManager().getByMember(target.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(viewer, "visit-no-island", "{player}", targetName);
            return;
        }
        visit(viewer, island);
    }

    public void visitOwner(Player viewer, UUID ownerId) {
        Island island = plugin.getIslandManager().getByOwner(ownerId);
        if (island == null) {
            plugin.getMessages().send(viewer, "visit-no-island", "{player}", nameOf(ownerId));
            return;
        }
        visit(viewer, island);
    }

    public void visit(Player viewer, Island island) {
        UUID playerId = viewer.getUniqueId();

        if (island.isBanned(playerId)) {
            plugin.getMessages().send(viewer, "visit-banned");
            return;
        }
        if (island.isLocked() && !island.isMember(playerId)) {
            plugin.getMessages().send(viewer, "visit-locked");
            return;
        }

        ProxyManager proxy = plugin.getProxyManager();
        if (proxy != null && proxy.handleTeleport(viewer, island))
            return;

        plugin.getMessages().send(viewer, "visiting", "{player}", nameOf(island.getOwner()));
        plugin.getIslandManager().teleportHome(viewer, island);
    }

    private OfflinePlayer resolveOffline(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null)
            return online;
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline : null;
    }

    private String nameOf(UUID id) {
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : id.toString().substring(0, 8);
    }
}
