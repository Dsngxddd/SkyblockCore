package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IslandCommand extends Command {

    private final SkyblockPlugin plugin;
    private final Map<String, String> resolver;

    private final GeneralCommands general;
    private final MemberCommands members;
    private final SettingsCommands settings;
    private final AdminCommands admin;

    public IslandCommand(SkyblockPlugin plugin, String name, List<String> aliases, Map<String, String> resolver) {
        super(name);
        this.plugin = plugin;
        this.resolver = resolver;
        this.general = new GeneralCommands(plugin);
        this.members = new MemberCommands(plugin);
        this.settings = new SettingsCommands(plugin);
        this.admin = new AdminCommands(plugin);
        setAliases(aliases);
        setDescription("Island commands");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessages().send(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            general.openMenu(player, "main");
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
            case "menu":      general.openMenu(player, "main"); break;
            case "git":       general.home(player); break;
            case "ziyaret":   general.visit(player, arg1); break;
            case "olustur":   general.create(player, arg1); break;
            case "sil":       general.delete(player); break;
            case "level":     general.level(player); break;
            case "bilgi":     general.info(player); break;
            case "uyeler":    general.members(player); break;
            case "yardim":    general.openMenu(player, "help"); break;

            case "davet":     members.invite(player, arg1); break;
            case "kabul":     members.accept(player); break;
            case "reddet":    members.deny(player); break;
            case "ayril":     members.leave(player); break;
            case "at":        members.kick(player, arg1); break;
            case "devret":    members.transfer(player, arg1); break;
            case "ban":       members.ban(player, arg1); break;
            case "unban":     members.unban(player, arg1); break;
            case "trust":     members.trust(player, arg1); break;
            case "untrust":   members.untrust(player, arg1); break;
            case "rol":       members.setRole(player, arg1, arg2); break;

            case "ayarlar":   settings.openIslandMenu(player, "settings"); break;
            case "yukseltme": settings.openIslandMenu(player, "upgrades"); break;
            case "setspawn":  settings.setSpawn(player); break;
            case "ucus":      settings.toggleFly(player); break;
            case "kilit":     settings.toggleLock(player); break;

            case "proxy":     admin.proxyStatus(player); break;
            case "anaspawn":  admin.setGlobalSpawn(player); break;

            default:          plugin.getMessages().send(player, "unknown-subcommand"); break;
        }
        return true;
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
            case "ziyaret":
                return true;
            default:
                return false;
        }
    }
}
