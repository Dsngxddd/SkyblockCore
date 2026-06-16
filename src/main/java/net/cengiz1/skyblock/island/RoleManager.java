package net.cengiz1.skyblock.island;

import net.cengiz1.skyblock.SkyblockPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the shared built-in roles loaded from roles.yml and resolves role ids.
 * Per-island custom roles live on the {@link Island} itself.
 */
public class RoleManager implements RoleResolver {

    public static final String VISITOR_ID = "visitor";
    public static final String MEMBER_ID = "member";
    public static final String OWNER_ID = "owner";

    private final SkyblockPlugin plugin;
    private final Map<String, RoleData> builtins = new LinkedHashMap<>();

    public RoleManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.builtins.clear();


        for (IslandRole role : IslandRole.values()) {
            RoleData data = new RoleData(role.name().toLowerCase(), role.getDisplayName(), role.getWeight(), true);
            data.setPermissions(role.getPermissions());
            this.builtins.put(data.getId(), data);
        }

        File file = new File(plugin.getDataFolder(), "roles.yml");
        if (!file.exists())
            plugin.saveResource("roles.yml", false);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection roles = config.getConfigurationSection("roles");
        if (roles != null) {
            for (RoleData data : this.builtins.values()) {
                ConfigurationSection section = roles.getConfigurationSection(data.getId());
                if (section == null)
                    continue;

                data.setDisplayName(section.getString("name"));

                if (data.getId().equals(OWNER_ID)) {
                    data.setPermissions(EnumSet.allOf(IslandPermission.class));
                    continue;
                }

                if (section.isList("permissions")) {
                    List<String> list = section.getStringList("permissions");
                    EnumSet<IslandPermission> set = EnumSet.noneOf(IslandPermission.class);
                    if (list.size() == 1 && list.get(0).equalsIgnoreCase("*")) {
                        set = EnumSet.allOf(IslandPermission.class);
                    } else {
                        for (String entry : list) {
                            IslandPermission permission = IslandPermission.fromString(entry);
                            if (permission != null)
                                set.add(permission);
                        }
                    }
                    data.setPermissions(set);
                }
            }
        }


        RoleData owner = this.builtins.get(OWNER_ID);
        if (owner != null)
            owner.setPermissions(EnumSet.allOf(IslandPermission.class));
    }

    public Collection<RoleData> getBuiltins() {
        return Collections.unmodifiableCollection(builtins.values());
    }

    /** Built-in roles a member can be assigned to (everything except visitor/owner). */
    public java.util.List<RoleData> assignableBuiltins() {
        java.util.List<RoleData> result = new java.util.ArrayList<>();
        for (RoleData data : builtins.values())
            if (!data.getId().equals(VISITOR_ID) && !data.getId().equals(OWNER_ID))
                result.add(data);
        return result;
    }

    @Override
    public RoleData builtin(String id) {
        return id == null ? null : builtins.get(id.toLowerCase());
    }

    @Override
    public RoleData owner() {
        return builtins.get(OWNER_ID);
    }

    @Override
    public RoleData visitor() {
        return builtins.get(VISITOR_ID);
    }

    @Override
    public RoleData defaultMember() {
        return builtins.get(MEMBER_ID);
    }
}
