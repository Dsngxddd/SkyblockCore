package net.cengiz1.skyblock.island;

import net.cengiz1.skyblock.SkyblockPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumSet;
import java.util.List;

public class RoleManager {

    private final SkyblockPlugin plugin;

    public RoleManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "roles.yml");
        if (!file.exists())
            plugin.saveResource("roles.yml", false);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection roles = config.getConfigurationSection("roles");
        if (roles == null)
            return;

        for (IslandRole role : IslandRole.values()) {
            ConfigurationSection section = roles.getConfigurationSection(role.name().toLowerCase());
            if (section == null)
                continue;

            String name = section.getString("name");
            if (name != null)
                role.setDisplayName(name);

            if (role == IslandRole.OWNER)
                continue;

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
                role.setPermissions(set);
            }
        }
    }
}
