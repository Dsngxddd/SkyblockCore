package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandPermission;
import net.cengiz1.skyblock.island.IslandRole;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MemberCommands extends CommandHandler {

    public MemberCommands(SkyblockPlugin plugin) {
        super(plugin);
    }

    public void invite(Player player, String targetName) {
        if (targetName == null) {
            plugin.getMessages().send(player, "usage-invite");
            return;
        }
        Island island = requirePermission(player, IslandPermission.INVITE);
        if (island == null)
            return;

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            plugin.getMessages().send(player, "player-not-found", "{player}", targetName);
            return;
        }
        if (island.isMember(target.getUniqueId())) {
            plugin.getMessages().send(player, "already-member");
            return;
        }
        int limit = (int) plugin.getUpgradeManager().getValue(island, "team-limit", 4);
        if (island.getMemberCount() >= limit) {
            plugin.getMessages().send(player, "team-limit-reached", "{limit}", String.valueOf(limit));
            return;
        }

        plugin.getInviteManager().invite(target.getUniqueId(), island.getUniqueId(), player.getUniqueId());
        plugin.getMessages().send(player, "invite-sent", "{player}", target.getName());
        plugin.getMessages().send(target, "invite-received",
                "{player}", player.getName(),
                "{command}", "/" + plugin.getSettings().getCommandName() + " kabul");
    }

    public void accept(Player player) {
        if (plugin.getIslandManager().getByMember(player.getUniqueId()) != null) {
            plugin.getMessages().send(player, "already-have-island");
            return;
        }
        UUID islandId = plugin.getInviteManager().consume(player.getUniqueId());
        if (islandId == null) {
            plugin.getMessages().send(player, "no-invite");
            return;
        }
        Island island = plugin.getIslandManager().getById(islandId);
        if (island == null) {
            plugin.getMessages().send(player, "no-invite");
            return;
        }
        island.addMember(player.getUniqueId());
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "invite-accepted");
        plugin.getIslandManager().messageMembers(island, "member-joined", "{player}", player.getName());
    }

    public void deny(Player player) {
        if (!plugin.getInviteManager().hasInvite(player.getUniqueId())) {
            plugin.getMessages().send(player, "no-invite");
            return;
        }
        plugin.getInviteManager().cancel(player.getUniqueId());
        plugin.getMessages().send(player, "invite-denied");
    }

    public void leave(Player player) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        if (island.isOwner(player.getUniqueId())) {
            plugin.getMessages().send(player, "owner-cannot-leave");
            return;
        }
        island.removeMember(player.getUniqueId());
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "left-island");
        plugin.getIslandManager().messageMembers(island, "member-left", "{player}", player.getName());
    }

    public void kick(Player player, String targetName) {
        if (targetName == null) {
            plugin.getMessages().send(player, "usage-kick");
            return;
        }
        Island island = requirePermission(player, IslandPermission.KICK);
        if (island == null)
            return;

        OfflinePlayer target = resolveOffline(targetName);
        if (target == null || !island.isMember(target.getUniqueId())) {
            plugin.getMessages().send(player, "not-a-member");
            return;
        }
        if (island.isOwner(target.getUniqueId())) {
            plugin.getMessages().send(player, "cannot-kick-owner");
            return;
        }
        if (!island.getRole(player.getUniqueId()).canManage(island.getRole(target.getUniqueId()))) {
            plugin.getMessages().send(player, "cannot-manage-higher");
            return;
        }
        island.removeMember(target.getUniqueId());
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "kicked", "{player}", target.getName());
        Player online = target.getPlayer();
        if (online != null)
            plugin.getMessages().send(online, "you-were-kicked");
    }

    public void transfer(Player player, String targetName) {
        if (targetName == null) {
            plugin.getMessages().send(player, "usage-transfer");
            return;
        }
        Island island = plugin.getIslandManager().getByOwner(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !island.isMember(target.getUniqueId()) || island.isOwner(target.getUniqueId())) {
            plugin.getMessages().send(player, "not-a-member");
            return;
        }
        plugin.getIslandManager().transferOwnership(island, target.getUniqueId());
        plugin.getMessages().send(player, "transferred", "{player}", target.getName());
        plugin.getMessages().send(target, "received-ownership");
    }

    public void ban(Player player, String targetName) {
        if (targetName == null) {
            plugin.getMessages().send(player, "usage-ban");
            return;
        }
        Island island = requirePermission(player, IslandPermission.BAN);
        if (island == null)
            return;
        OfflinePlayer target = resolveOffline(targetName);
        if (target == null) {
            plugin.getMessages().send(player, "player-not-found", "{player}", targetName);
            return;
        }
        if (island.isOwner(target.getUniqueId())) {
            plugin.getMessages().send(player, "cannot-ban-owner");
            return;
        }
        island.ban(target.getUniqueId());
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "banned", "{player}", target.getName());
    }

    public void unban(Player player, String targetName) {
        if (targetName == null) {
            plugin.getMessages().send(player, "usage-unban");
            return;
        }
        Island island = requirePermission(player, IslandPermission.BAN);
        if (island == null)
            return;
        OfflinePlayer target = resolveOffline(targetName);
        if (target == null || !island.isBanned(target.getUniqueId())) {
            plugin.getMessages().send(player, "not-banned");
            return;
        }
        island.unban(target.getUniqueId());
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "unbanned", "{player}", target.getName());
    }

    public void trust(Player player, String targetName) {
        if (targetName == null) {
            plugin.getMessages().send(player, "usage-trust");
            return;
        }
        Island island = requirePermission(player, IslandPermission.INVITE);
        if (island == null)
            return;
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            plugin.getMessages().send(player, "player-not-found", "{player}", targetName);
            return;
        }
        if (island.isMember(target.getUniqueId())) {
            plugin.getMessages().send(player, "already-member");
            return;
        }
        int limit = (int) plugin.getUpgradeManager().getValue(island, "team-limit", 4);
        if (island.getMemberCount() >= limit) {
            plugin.getMessages().send(player, "team-limit-reached", "{limit}", String.valueOf(limit));
            return;
        }
        island.addMember(target.getUniqueId());
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "trusted", "{player}", target.getName());
        plugin.getMessages().send(target, "you-were-trusted");
    }

    public void untrust(Player player, String targetName) {
        if (targetName == null) {
            plugin.getMessages().send(player, "usage-untrust");
            return;
        }
        Island island = requirePermission(player, IslandPermission.KICK);
        if (island == null)
            return;
        OfflinePlayer target = resolveOffline(targetName);
        if (target == null || !island.isMember(target.getUniqueId()) || island.isOwner(target.getUniqueId())) {
            plugin.getMessages().send(player, "not-a-member");
            return;
        }
        island.removeMember(target.getUniqueId());
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "untrusted", "{player}", target.getName());
    }

    public void setRole(Player player, String targetName, String roleName) {
        if (targetName == null || roleName == null) {
            plugin.getMessages().send(player, "usage-role");
            return;
        }
        Island island = requirePermission(player, IslandPermission.MANAGE_MEMBERS);
        if (island == null)
            return;
        OfflinePlayer target = resolveOffline(targetName);
        if (target == null || !island.isMember(target.getUniqueId()) || island.isOwner(target.getUniqueId())) {
            plugin.getMessages().send(player, "not-a-member");
            return;
        }
        IslandRole role = IslandRole.fromString(roleName);
        if (role == null || role == IslandRole.OWNER || role == IslandRole.VISITOR) {
            plugin.getMessages().send(player, "invalid-role", "{roles}", joinRoles());
            return;
        }
        IslandRole actorRole = island.getRole(player.getUniqueId());
        if (!actorRole.canManage(island.getRole(target.getUniqueId())) || !actorRole.canManage(role)) {
            plugin.getMessages().send(player, "cannot-manage-higher");
            return;
        }
        island.setRole(target.getUniqueId(), role);
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "role-set",
                "{player}", target.getName(), "{role}", role.getDisplayName());
    }
}
