package net.cengiz1.skyblock.menu;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandFlag;
import net.cengiz1.skyblock.island.IslandManager;
import net.cengiz1.skyblock.island.IslandPermission;
import net.cengiz1.skyblock.island.IslandTime;
import net.cengiz1.skyblock.upgrade.Upgrade;
import net.cengiz1.skyblock.upgrade.UpgradeLevel;
import net.cengiz1.skyblock.upgrade.UpgradeManager;
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

    public MenuManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
        this.islandManager = plugin.getIslandManager();
        reload();
    }

    public void reload() {
        this.menus.clear();

        File folder = new File(plugin.getDataFolder(), "menus");
        if (!folder.exists()) {
            folder.mkdirs();
            plugin.saveResource("menus/main.yml", false);
            plugin.saveResource("menus/settings.yml", false);
            plugin.saveResource("menus/upgrades.yml", false);
            plugin.saveResource("menus/help.yml", false);
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            this.menus.put(id, parse(config));
        }
        plugin.getLogger().info(this.menus.size() + " menü yüklendi.");
    }

    private MenuDefinition parse(YamlConfiguration config) {
        String title = config.getString("title", "Menü");
        int rows = Math.max(1, Math.min(6, config.getInt("rows", 3)));
        String type = config.getString("type", "normal");
        MenuDefinition definition = new MenuDefinition(title, rows, type);

        definition.setUpgradeLore(config.getStringList("upgrade-lore"));
        definition.setUpgradeLoreMax(config.getStringList("upgrade-lore-max"));
        definition.setBlockValuesFormat(config.getString("block-values-format"));

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
                boolean blockValues = entry.getBoolean("block-values", false);
                definition.getEntries().add(
                        new MenuDefinition.Entry(slot, material, amount, name, lore, action, blockValues));
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
                : islandManager.getByMember(player.getUniqueId());

        MenuHolder holder = new MenuHolder(menuId.toLowerCase(), island != null ? island.getUniqueId() : null);
        Inventory inventory = Bukkit.createInventory(holder, definition.getRows() * 9,
                color(apply(definition.getTitle(), player, island)));
        holder.setInventory(inventory);

        // Statik öğeler
        for (MenuDefinition.Entry entry : definition.getEntries()) {
            if (entry.slot < 0 || entry.slot >= inventory.getSize())
                continue;
            inventory.setItem(entry.slot, buildItem(definition, entry, player, island));
            if (entry.action != null && !entry.action.isEmpty())
                holder.getActions().put(entry.slot, entry.action);
        }

        // Yükseltme menüsü: otomatik doldur
        if (definition.getType().equals("upgrades") && island != null)
            populateUpgrades(definition, inventory, holder, player, island);

        player.openInventory(inventory);
    }

    private void populateUpgrades(MenuDefinition definition, Inventory inventory, MenuHolder holder,
                                  Player player, Island island) {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        for (Upgrade upgrade : upgrades.getUpgrades().values()) {
            int slot = upgrade.getSlot();
            if (slot < 0 || slot >= inventory.getSize())
                continue;

            int currentLevel = island.getUpgradeLevel(upgrade.getKey());
            UpgradeLevel current = upgrade.getLevel(currentLevel);
            UpgradeLevel next = upgrade.getNextLevel(currentLevel);
            boolean maxed = next == null;

            Material icon = upgrade.getIcon();
            if (current != null && current.getIcon() != null)
                icon = current.getIcon();

            ItemStack item = new ItemStack(maxed && icon == null ? Material.BARRIER : icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(upgrade.getDisplayName() + " &7[" + currentLevel + "]"));
                List<String> template = maxed && !definition.getUpgradeLoreMax().isEmpty()
                        ? definition.getUpgradeLoreMax()
                        : definition.getUpgradeLore();
                List<String> lore = new ArrayList<>();
                for (String line : template)
                    lore.add(color(applyUpgrade(line, upgrade, current, next)));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            if (!maxed)
                holder.getActions().put(slot, "upgrade:" + upgrade.getKey());
        }
    }

    public void handleAction(Player player, MenuHolder holder, String action) {
        String lower = action.toLowerCase();
        if (lower.equals("close")) {
            player.closeInventory();
            return;
        }
        if (lower.startsWith("open:")) {
            open(player, action.substring(5).trim(), holder.getIslandId());
            return;
        }
        if (lower.startsWith("command:")) {
            player.closeInventory();
            player.performCommand(action.substring(8).trim());
            return;
        }
        if (lower.startsWith("island:")) {
            player.closeInventory();
            player.performCommand(plugin.getSettings().getCommandName() + " " + action.substring(7).trim());
            return;
        }
        if (lower.startsWith("flag:")) {
            handleFlagToggle(player, holder, action.substring(5).trim());
            return;
        }
        if (lower.startsWith("upgrade:")) {
            handleUpgrade(player, holder, action.substring(8).trim());
            return;
        }
        if (lower.equals("time")) {
            handleTimeToggle(player, holder);
        }
    }

    private void handleFlagToggle(Player player, MenuHolder holder, String flagName) {
        IslandFlag flag = IslandFlag.fromString(flagName);
        if (flag == null)
            return;
        Island island = resolveIsland(player, holder);
        if (island == null || !island.hasPermission(player.getUniqueId(), IslandPermission.TOGGLE_SETTINGS)) {
            plugin.getMessages().send(player, "no-island-permission");
            return;
        }
        island.setFlag(flag, !island.getFlag(flag));
        islandManager.saveAsync(island);
        open(player, holder.getMenuId(), island.getUniqueId());
    }

    private void handleTimeToggle(Player player, MenuHolder holder) {
        Island island = resolveIsland(player, holder);
        if (island == null || !island.hasPermission(player.getUniqueId(), IslandPermission.TOGGLE_SETTINGS)) {
            plugin.getMessages().send(player, "no-island-permission");
            return;
        }
        if (!player.hasPermission("skyblock.time")) {
            plugin.getMessages().send(player, "time-no-permission");
            return;
        }
        island.setTime(island.getTime().next());
        islandManager.saveAsync(island);
        open(player, holder.getMenuId(), island.getUniqueId());
    }

    private void handleUpgrade(Player player, MenuHolder holder, String key) {
        Island island = resolveIsland(player, holder);
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        if (!island.hasPermission(player.getUniqueId(), IslandPermission.UPGRADE)) {
            plugin.getMessages().send(player, "no-island-permission");
            return;
        }
        UpgradeManager.PurchaseResult result =
                plugin.getUpgradeManager().purchase(player, island, key, plugin.getEconomy());
        switch (result) {
            case SUCCESS:
                plugin.getMessages().send(player, "upgrade-success");
                break;
            case MAX_LEVEL:
                plugin.getMessages().send(player, "upgrade-max");
                break;
            case NEED_ISLAND_LEVEL:
                plugin.getMessages().send(player, "upgrade-need-level");
                break;
            case NEED_MONEY:
                plugin.getMessages().send(player, "upgrade-need-money");
                break;
            default:
                plugin.getMessages().send(player, "upgrade-failed");
                break;
        }
        open(player, holder.getMenuId(), island.getUniqueId());
    }

    private Island resolveIsland(Player player, MenuHolder holder) {
        return holder.getIslandId() != null
                ? islandManager.getById(holder.getIslandId())
                : islandManager.getByMember(player.getUniqueId());
    }

    private ItemStack buildItem(MenuDefinition definition, MenuDefinition.Entry entry, Player player, Island island) {
        ItemStack item = new ItemStack(entry.material, Math.max(1, entry.amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (entry.name != null && !entry.name.isEmpty())
                meta.setDisplayName(color(apply(entry.name, player, island)));

            List<String> lore = new ArrayList<>();
            if (entry.lore != null)
                for (String line : entry.lore)
                    lore.add(color(apply(line, player, island)));

            if (entry.blockValues)
                for (Map.Entry<Material, Double> bv : plugin.getBlockValueManager().getPositiveValues().entrySet())
                    lore.add(color(definition.getBlockValuesFormat()
                            .replace("{block}", prettyName(bv.getKey().name()))
                            .replace("{value}", formatNumber(bv.getValue()))));

            if (!lore.isEmpty())
                meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String applyUpgrade(String text, Upgrade upgrade, UpgradeLevel current, UpgradeLevel next) {
        if (text == null)
            return "";
        String result = text;
        result = result.replace("{upgrade}", upgrade.getDisplayName());
        result = result.replace("{current}", current != null ? formatNumber(current.getValue()) : "-");
        result = result.replace("{next}", next != null ? formatNumber(next.getValue()) : "-");
        result = result.replace("{req_level}", next != null ? String.valueOf(next.getRequiredIslandLevel()) : "-");
        result = result.replace("{req_money}", next != null ? formatNumber(next.getRequiredMoney()) : "-");
        result = result.replace("{max}", String.valueOf(upgrade.getMaxLevel()));
        return result;
    }

    private String apply(String text, Player player, Island island) {
        if (text == null)
            return "";
        String result = text.replace("{player}", player.getName());
        result = result.replace("{owner}", island != null ? nameOf(island.getOwner()) : "-");
        result = result.replace("{level}", island != null ? String.valueOf(island.getLevel()) : "0");
        result = result.replace("{points}", island != null ? formatNumber(island.getPoints()) : "0");
        result = result.replace("{island_name}",
                island != null && island.getName() != null ? island.getName() : nameOf(island != null ? island.getOwner() : player.getUniqueId()));
        result = result.replace("{members}", island != null ? String.valueOf(island.getMemberCount()) : "0");
        result = result.replace("{team_limit}", island != null
                ? String.valueOf((int) plugin.getUpgradeManager().getValue(island, "team-limit", 4)) : "0");
        result = result.replace("{time}", island != null ? island.getTime().getDisplayName() : IslandTime.NORMAL.getDisplayName());

        double next = island != null ? plugin.getLevelManager().pointsForNextLevel(island.getLevel()) : -1;
        result = result.replace("{next_points}", next < 0 ? "MAX" : formatNumber(next));

        for (IslandFlag flag : IslandFlag.values()) {
            boolean value = island != null ? island.getFlag(flag) : flag.getDefault();
            result = result.replace("{flag_" + flag.name().toLowerCase() + "}", value ? "&aAÇIK" : "&cKAPALI");
        }
        return result;
    }

    private String nameOf(UUID id) {
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : id.toString().substring(0, 8);
    }

    private String prettyName(String enumName) {
        String[] parts = enumName.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0)
                builder.append(' ');
            if (!part.isEmpty())
                builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value))
            return String.valueOf((long) value);
        return String.format("%.1f", value);
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
