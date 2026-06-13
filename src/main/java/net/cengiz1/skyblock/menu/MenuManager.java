package net.cengiz1.skyblock.menu;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandFlag;
import net.cengiz1.skyblock.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MenuManager {

    private final SkyblockPlugin plugin;
    private final IslandManager islandManager;
    private final Map<String, MenuDefinition> menus = new ConcurrentHashMap<>();

    public MenuManager(SkyblockPlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        reload();
    }

    public void reload() {
        this.menus.clear();

        File folder = new File(plugin.getDataFolder(), "menus");
        if (!folder.exists()) {
            folder.mkdirs();
            plugin.saveResource("menus/main.yml", false);
            plugin.saveResource("menus/settings.yml", false);
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            this.menus.put(id, parse(config));
        }

        plugin.getLogger().info("Loaded " + this.menus.size() + " menus.");
    }

    private MenuDefinition parse(YamlConfiguration config) {
        String title = config.getString("title", "Menu");
        int rows = Math.max(1, Math.min(6, config.getInt("rows", 3)));
        MenuDefinition definition = new MenuDefinition(title, rows);

        ConfigurationSection items = config.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection entry = items.getConfigurationSection(key);
                if (entry == null)
                    continue;

                int slot = entry.getInt("slot", 0);
                Material material = Material.matchMaterial(entry.getString("material", "STONE"));
                if (material == null)
                    material = Material.STONE;
                int amount = entry.getInt("amount", 1);
                String name = entry.getString("name", "");
                List<String> lore = entry.getStringList("lore");
                String action = entry.getString("action", null);

                definition.getEntries().add(new MenuDefinition.Entry(slot, material, amount, name, lore, action));
            }
        }
        return definition;
    }

    public boolean has(String menuId) {
        return menuId != null && this.menus.containsKey(menuId.toLowerCase());
    }

    public void open(Player player, String menuId, UUID islandId) {
        MenuDefinition definition = this.menus.get(menuId.toLowerCase());
        if (definition == null)
            return;

        Island island = islandId != null
                ? islandManager.getById(islandId)
                : islandManager.getByOwner(player.getUniqueId());

        MenuHolder holder = new MenuHolder(menuId.toLowerCase(), island != null ? island.getUniqueId() : null);
        Inventory inventory = Bukkit.createInventory(holder, definition.getRows() * 9,
                color(apply(definition.getTitle(), player, island)));
        holder.setInventory(inventory);

        for (MenuDefinition.Entry entry : definition.getEntries()) {
            if (entry.slot < 0 || entry.slot >= inventory.getSize())
                continue;
            inventory.setItem(entry.slot, buildItem(entry, player, island));
            if (entry.action != null && !entry.action.isEmpty())
                holder.getActions().put(entry.slot, entry.action);
        }

        player.openInventory(inventory);
    }

    public void handleAction(Player player, MenuHolder holder, String action) {
        if (action.equalsIgnoreCase("close")) {
            player.closeInventory();
            return;
        }
        if (action.toLowerCase().startsWith("open:")) {
            open(player, action.substring(5).trim(), holder.getIslandId());
            return;
        }
        if (action.toLowerCase().startsWith("command:")) {
            player.closeInventory();
            player.performCommand(action.substring(8).trim());
            return;
        }
        if (action.toLowerCase().startsWith("island:")) {
            String sub = action.substring(7).trim();
            player.closeInventory();
            player.performCommand("island " + sub);
            return;
        }
        if (action.toLowerCase().startsWith("flag:")) {
            handleFlagToggle(player, holder, action.substring(5).trim());
        }
    }

    private void handleFlagToggle(Player player, MenuHolder holder, String flagName) {
        IslandFlag flag = IslandFlag.fromString(flagName);
        if (flag == null)
            return;

        Island island = holder.getIslandId() != null
                ? islandManager.getById(holder.getIslandId())
                : islandManager.getByOwner(player.getUniqueId());

        if (island == null || !island.getOwner().equals(player.getUniqueId()))
            return;

        island.setFlag(flag, !island.getFlag(flag));
        islandManager.saveAsync(island);
        open(player, holder.getMenuId(), island.getUniqueId());
    }

    private ItemStack buildItem(MenuDefinition.Entry entry, Player player, Island island) {
        ItemStack item = new ItemStack(entry.material, Math.max(1, entry.amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (entry.name != null && !entry.name.isEmpty())
                meta.setDisplayName(color(apply(entry.name, player, island)));
            if (entry.lore != null && !entry.lore.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : entry.lore)
                    lore.add(color(apply(line, player, island)));
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String apply(String text, Player player, Island island) {
        if (text == null)
            return "";

        String result = text.replace("{player}", player.getName());
        result = result.replace("{owner}", island != null ? nameOf(island.getOwner()) : "-");

        for (IslandFlag flag : IslandFlag.values()) {
            boolean value = island != null ? island.getFlag(flag) : flag.getDefault();
            result = result.replace("{flag_" + flag.name().toLowerCase() + "}", value ? "&aON" : "&cOFF");
        }
        return result;
    }

    private String nameOf(UUID id) {
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : id.toString().substring(0, 8);
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
