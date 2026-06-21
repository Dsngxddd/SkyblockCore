package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandPermission;
import net.cengiz1.skyblock.island.RoleData;
import net.cengiz1.skyblock.island.RoleManager;
import org.bukkit.entity.Player;

import java.util.StringJoiner;

/**
 * Custom island role management: /island role create|delete|perm|list, plus the
 * existing assignment form /island role &lt;player&gt; &lt;role&gt; (delegated to
 * {@link MemberCommands}). Built-in roles (roles.yml) are read-only here.
 */
public class RoleCommands extends CommandHandler {

    private final MemberCommands members;

    public RoleCommands(SkyblockPlugin plugin, MemberCommands members) {
        super(plugin);
        this.members = members;
    }

    public static boolean isManagement(String sub) {
        if (sub == null)
            return false;
        switch (sub.toLowerCase(java.util.Locale.ROOT)) {
            case "olustur": case "oluştur": case "create": case "ekle":
            case "sil": case "delete": case "kaldir": case "kaldır":
            case "perm": case "izin":
            case "list": case "liste": case "roller":
                return true;
            default:
                return false;
        }
    }

    public void handle(Player player, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase(java.util.Locale.ROOT) : null;
        if (sub == null) {
            plugin.getMessages().send(player, "role-usage");
            return;
        }
        switch (sub) {
            case "olustur": case "oluştur": case "create": case "ekle":
                create(player, args);
                break;
            case "sil": case "delete": case "kaldir": case "kaldır":
                delete(player, args);
                break;
            case "perm": case "izin":
                permission(player, args);
                break;
            case "list": case "liste": case "roller":
                list(player);
                break;
            default:
                plugin.getMessages().send(player, "role-usage");
        }
    }

    private void create(Player player, String[] args) {
        Island island = requirePermission(player, IslandPermission.MANAGE_MEMBERS);
        if (island == null)
            return;
        if (args.length < 3) {
            plugin.getMessages().send(player, "role-create-usage");
            return;
        }
        String id = sanitizeId(args[2]);
        if (id == null) {
            plugin.getMessages().send(player, "role-create-usage");
            return;
        }
        if (isBuiltinId(id)) {
            plugin.getMessages().send(player, "role-builtin-protected");
            return;
        }
        if (island.hasCustomRole(id)) {
            plugin.getMessages().send(player, "role-exists", "{role}", id);
            return;
        }

        String display = args.length >= 4 ? joinFrom(args, 3) : id;
        int weight = plugin.getRoleManager().defaultMember() != null
                ? plugin.getRoleManager().defaultMember().getWeight() : 2;
        island.createCustomRole(id, display, weight);
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "role-created", "{role}", id);
    }

    private void delete(Player player, String[] args) {
        Island island = requirePermission(player, IslandPermission.MANAGE_MEMBERS);
        if (island == null)
            return;
        if (args.length < 3) {
            plugin.getMessages().send(player, "role-usage");
            return;
        }
        String id = sanitizeId(args[2]);
        if (id != null && isBuiltinId(id)) {
            plugin.getMessages().send(player, "role-builtin-protected");
            return;
        }
        if (id == null || !island.removeCustomRole(id)) {
            plugin.getMessages().send(player, "role-not-found", "{role}", args[2]);
            return;
        }
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "role-deleted", "{role}", id);
    }

    private void permission(Player player, String[] args) {
        Island island = requirePermission(player, IslandPermission.MANAGE_MEMBERS);
        if (island == null)
            return;
        if (args.length < 5) {
            plugin.getMessages().send(player, "role-perm-usage", "{perms}", joinPermissions());
            return;
        }
        String id = sanitizeId(args[2]);
        RoleData role = id == null ? null : island.getCustomRole(id);
        if (role == null) {
            plugin.getMessages().send(player, "role-not-found", "{role}", args[2]);
            return;
        }
        IslandPermission permission = IslandPermission.fromString(args[3]);
        if (permission == null) {
            plugin.getMessages().send(player, "role-invalid-perm", "{perms}", joinPermissions());
            return;
        }
        boolean value = isOn(args[4]);
        role.setPermission(permission, value);
        island.markDirty();
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "role-perm-set",
                "{role}", role.getId(),
                "{perm}", permission.name(),
                "{state}", plugin.getMessages().get(value ? "flag-on" : "flag-off"));
    }

    private void list(Player player) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        plugin.getMessages().send(player, "role-list-header");
        for (RoleData role : plugin.getRoleManager().getBuiltins())
            plugin.getMessages().send(player, "role-list-builtin",
                    "{role}", role.getId(), "{display}", role.getDisplayName());
        for (RoleData role : island.getCustomRoles())
            plugin.getMessages().send(player, "role-list-custom",
                    "{role}", role.getId(),
                    "{display}", role.getDisplayName(),
                    "{perms}", joinRolePermissions(role));
    }

    private boolean isBuiltinId(String id) {
        return plugin.getRoleManager().builtin(id) != null;
    }

    private boolean isOn(String arg) {
        if (arg == null)
            return false;
        switch (arg.toLowerCase(java.util.Locale.ROOT)) {
            case "on": case "true": case "ac": case "aç": case "1": case "yes": case "evet":
                return true;
            default:
                return false;
        }
    }

    private String sanitizeId(String name) {
        if (name == null)
            return null;
        String cleaned = name.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String joinFrom(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0)
                builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private String joinPermissions() {
        StringJoiner joiner = new StringJoiner(", ");
        for (IslandPermission permission : IslandPermission.values())
            joiner.add(permission.name());
        return joiner.toString();
    }

    private String joinRolePermissions(RoleData role) {
        if (role.getPermissions().isEmpty())
            return "-";
        StringJoiner joiner = new StringJoiner(", ");
        for (IslandPermission permission : role.getPermissions())
            joiner.add(permission.name());
        return joiner.toString();
    }
}
