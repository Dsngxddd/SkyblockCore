package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandManager;
import net.cengiz1.skyblock.schematic.SchematicDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.StringJoiner;

public class IslandCommand implements CommandExecutor {

    private final SkyblockPlugin plugin;

    public IslandCommand(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessages().send(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            openMenu(player, "main");
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create":
                handleCreate(player, args.length >= 2 ? args[1] : null);
                return true;
            case "home":
            case "go":
            case "tp":
                handleHome(player);
                return true;
            case "delete":
                handleDelete(player);
                return true;
            case "types":
                handleTypes(player);
                return true;
            case "menu":
                openMenu(player, "main");
                return true;
            case "settings":
                handleSettings(player);
                return true;
            default:
                plugin.getMessages().send(player, "unknown-subcommand");
                return true;
        }
    }

    private void openMenu(Player player, String menuId) {
        if (!plugin.getMenuManager().has(menuId)) {
            plugin.getMessages().send(player, "unknown-subcommand");
            return;
        }
        plugin.getMenuManager().open(player, menuId, null);
    }

    private void handleSettings(Player player) {
        Island island = plugin.getIslandManager().getByOwner(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }
        plugin.getMenuManager().open(player, "settings", island.getUniqueId());
    }

    private void handleCreate(Player player, String type) {
        IslandManager manager = plugin.getIslandManager();

        if (manager.getByOwner(player.getUniqueId()) != null) {
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
                        case SUCCESS:
                            plugin.getMessages().send(player, "created");
                            break;
                        case ALREADY_HAS_ISLAND:
                            plugin.getMessages().send(player, "already-have-island");
                            break;
                        case ALREADY_CREATING:
                            plugin.getMessages().send(player, "creating-in-progress");
                            break;
                        case INVALID_SCHEMATIC:
                            plugin.getMessages().send(player, "invalid-schematic", "{types}", joinTypes());
                            break;
                        default:
                            plugin.getMessages().send(player, "create-failed");
                            break;
                    }
                }));
    }

    private void handleHome(Player player) {
        IslandManager manager = plugin.getIslandManager();
        Island island = manager.getByOwner(player.getUniqueId());

        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }

        plugin.getMessages().send(player, "teleporting");
        manager.teleportHome(player, island);
    }

    private void handleDelete(Player player) {
        IslandManager manager = plugin.getIslandManager();
        Island island = manager.getByOwner(player.getUniqueId());

        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }

        manager.deleteIsland(island);
        plugin.getMessages().send(player, "deleted");
    }

    private void handleTypes(Player player) {
        plugin.getMessages().send(player, "types-header");
        for (SchematicDefinition definition : plugin.getIslandManager().getSchematicService().getDefinitions())
            plugin.getMessages().send(player, "types-entry",
                    "{key}", definition.getKey(),
                    "{name}", definition.getDisplayName());
    }

    private String joinTypes() {
        StringJoiner joiner = new StringJoiner(", ");
        for (SchematicDefinition definition : plugin.getIslandManager().getSchematicService().getDefinitions())
            joiner.add(definition.getKey());
        return joiner.toString();
    }

    private void sendHelp(Player player) {
        plugin.getMessages().send(player, "help-header");
        plugin.getMessages().send(player, "help-create");
        plugin.getMessages().send(player, "help-home");
        plugin.getMessages().send(player, "help-delete");
        plugin.getMessages().send(player, "help-types");
        plugin.getMessages().send(player, "help-settings");
    }
}
