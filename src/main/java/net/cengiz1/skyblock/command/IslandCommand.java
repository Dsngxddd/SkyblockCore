package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandManager;
import net.cengiz1.skyblock.island.IslandPermission;
import net.cengiz1.skyblock.island.IslandRole;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Tüm ada komutlarını işler. Komut adı, alias'ları ve alt komut alias'ları
 * config.yml'den okunur (bkz: CommandRegistrar).
 */
public class IslandCommand extends Command {

    private final SkyblockPlugin plugin;
    // alias -> kanonik alt komut
    private final Map<String, String> resolver;

    public IslandCommand(SkyblockPlugin plugin, String name, List<String> aliases, Map<String, String> resolver) {
        super(name);
        this.plugin = plugin;
        this.resolver = resolver;
        setAliases(aliases);
        setDescription("Ada komutları");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessages().send(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            openMenu(player, "main");
            return true;
        }

        String canonical = this.resolver.getOrDefault(args[0].toLowerCase(), null);
        if (canonical == null) {
            plugin.getMessages().send(player, "unknown-subcommand");
            return true;
        }

        String arg1 = args.length >= 2 ? args[1] : null;
        String arg2 = args.length >= 3 ? args[2] : null;

        switch (canonical) {
            case "menu":      openMenu(player, "main"); break;
            case "git":       home(player); break;
            case "olustur":   create(player, arg1); break;
            case "sil":       delete(player); break;
            case "davet":     invite(player, arg1); break;
            case "kabul":     accept(player); break;
            case "reddet":    deny(player); break;
            case "ayril":     leave(player); break;
            case "at":        kick(player, arg1); break;
            case "devret":    transfer(player, arg1); break;
            case "ban":       ban(player, arg1); break;
            case "unban":     unban(player, arg1); break;
            case "trust":     trust(player, arg1); break;
            case "untrust":   untrust(player, arg1); break;
            case "rol":       setRole(player, arg1, arg2); break;
            case "ayarlar":   openIslandMenu(player, "settings"); break;
            case "yukseltme": openIslandMenu(player, "upgrades"); break;
            case "level":     levelInfo(player); break;
            case "setspawn":  setSpawn(player); break;
            case "ucus":      toggleFly(player); break;
            case "kilit":     toggleLock(player); break;
            case "bilgi":     info(player); break;
            case "uyeler":    members(player); break;
            case "yardim":    openMenu(player, "help"); break;
            default:          plugin.getMessages().send(player, "unknown-subcommand"); break;
        }
        return true;
    }

    // ───────────── Menü yardımcıları ─────────────

    private void openMenu(Player player, String menuId) {
        if (plugin.getMenuManager().has(menuId))
            plugin.getMenuManager().open(player, menuId, null);
        else
            plugin.getMessages().send(player, "unknown-subcommand");
    }

    private void openIslandMenu(Player player, String menuId) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        if (!plugin.getMenuManager().has(menuId)) {
            plugin.getMessages().send(player, "unknown-subcommand");
            return;
        }
        plugin.getMenuManager().open(player, menuId, island.getUniqueId());
    }

    // ───────────── Temel ─────────────

    private void home(Player player) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        plugin.getMessages().send(player, "teleporting");
        plugin.getIslandManager().teleportHome(player, island);
    }

    private void create(Player player, String type) {
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

    private void delete(Player player) {
        Island island = plugin.getIslandManager().getByOwner(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        plugin.getIslandManager().deleteIsland(island);
        plugin.getMessages().send(player, "deleted");
    }

    // ───────────── Üyelik / davet ─────────────

    private void invite(Player player, String targetName) {
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
                "{command}", "/" + getName() + " kabul");
    }

    private void accept(Player player) {
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

    private void deny(Player player) {
        if (!plugin.getInviteManager().hasInvite(player.getUniqueId())) {
            plugin.getMessages().send(player, "no-invite");
            return;
        }
        plugin.getInviteManager().cancel(player.getUniqueId());
        plugin.getMessages().send(player, "invite-denied");
    }

    private void leave(Player player) {
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

    private void kick(Player player, String targetName) {
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

    private void transfer(Player player, String targetName) {
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

    private void ban(Player player, String targetName) {
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

    private void unban(Player player, String targetName) {
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

    private void trust(Player player, String targetName) {
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

    private void untrust(Player player, String targetName) {
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

    private void setRole(Player player, String targetName, String roleName) {
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

    // ───────────── Bilgi / ayarlar ─────────────

    private void levelInfo(Player player) {
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

    private void setSpawn(Player player) {
        Island island = requirePermission(player, IslandPermission.SET_HOME);
        if (island == null)
            return;
        if (plugin.getIslandManager().getIslandAt(player.getLocation()) != island) {
            plugin.getMessages().send(player, "must-be-on-island");
            return;
        }
        island.setHome(player.getLocation());
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "spawn-set");
    }

    private void toggleFly(Player player) {
        Island island = requirePermission(player, IslandPermission.FLY);
        if (island == null)
            return;
        boolean enable = !player.getAllowFlight();
        player.setAllowFlight(enable);
        player.setFlying(enable);
        plugin.getMessages().send(player, enable ? "fly-on" : "fly-off");
    }

    private void toggleLock(Player player) {
        Island island = requirePermission(player, IslandPermission.TOGGLE_SETTINGS);
        if (island == null)
            return;
        island.setLocked(!island.isLocked());
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, island.isLocked() ? "island-locked" : "island-unlocked");
    }

    private void info(Player player) {
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

    private void members(Player player) {
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

    // ───────────── Yardımcılar ─────────────

    /** Oyuncunun adasını ve yetkisini kontrol eder; uygun değilse mesaj atıp null döner. */
    private Island requirePermission(Player player, IslandPermission permission) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return null;
        }
        if (!island.hasPermission(player.getUniqueId(), permission)) {
            plugin.getMessages().send(player, "no-island-permission");
            return null;
        }
        return island;
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

    private String formatNumber(double value) {
        if (value == Math.floor(value))
            return String.valueOf((long) value);
        return String.format("%.1f", value);
    }

    private String joinTypes() {
        StringJoiner joiner = new StringJoiner(", ");
        plugin.getIslandManager().getSchematicService().getDefinitions()
                .forEach(definition -> joiner.add(definition.getKey()));
        return joiner.toString();
    }

    private String joinRoles() {
        StringJoiner joiner = new StringJoiner(", ");
        for (IslandRole role : IslandRole.assignable())
            joiner.add(role.name().toLowerCase());
        return joiner.toString();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String key : this.resolver.keySet())
                if (key.startsWith(prefix))
                    result.add(key);
        } else if (args.length == 2) {
            String canonical = this.resolver.get(args[0].toLowerCase());
            if (canonical != null && isPlayerArg(canonical)) {
                String prefix = args[1].toLowerCase();
                for (Player online : Bukkit.getOnlinePlayers())
                    if (online.getName().toLowerCase().startsWith(prefix))
                        result.add(online.getName());
            }
        }
        return result;
    }

    private boolean isPlayerArg(String canonical) {
        switch (canonical) {
            case "davet": case "at": case "devret": case "ban":
            case "unban": case "trust": case "untrust": case "rol":
                return true;
            default:
                return false;
        }
    }
}
