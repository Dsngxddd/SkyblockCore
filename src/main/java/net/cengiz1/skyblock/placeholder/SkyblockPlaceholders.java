package net.cengiz1.skyblock.placeholder;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandFlag;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared resolver for the {@code %skyblock_...%} placeholders. Used both by the
 * PlaceholderAPI expansion ({@link SkyblockExpansion}) and directly by the menu
 * system so menus keep rendering correctly even when PlaceholderAPI is missing
 * or has not registered the expansion yet.
 */
public final class SkyblockPlaceholders {

    private static final Pattern TOKEN = Pattern.compile("%skyblock_([a-zA-Z0-9_]+)%");

    private SkyblockPlaceholders() {
    }

    /**
     * Resolve a single {@code skyblock_<key>} placeholder (without the % signs),
     * or {@code null} if the key is unknown.
     */
    public static String resolve(SkyblockPlugin plugin, OfflinePlayer player, String params) {
        if (player == null)
            return "";

        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        String key = params.toLowerCase(java.util.Locale.ROOT);

        if (key.equals("has_island"))
            return bool(plugin, island != null);

        if (island == null)
            return noIsland(plugin, key);

        if (key.startsWith("flag_")) {
            IslandFlag flag = IslandFlag.fromString(key.substring("flag_".length()));
            if (flag == null)
                return null;
            return flagState(plugin, island.getFlag(flag));
        }

        switch (key) {
            case "level":
                return String.valueOf(island.getLevel());
            case "points":
                return formatNumber(island.getPoints());
            case "bank":
                return formatNumber(island.getBank());
            case "next_points": {
                double next = plugin.getLevelManager().pointsForNextLevel(island.getLevel());
                return next < 0 ? "MAX" : formatNumber(next);
            }
            case "members":
                return String.valueOf(island.getMemberCount());
            case "team_limit":
                return String.valueOf((int) plugin.getUpgradeManager().getValue(island, "team-limit", 4));
            case "owner":
                return nameOf(island);
            case "name":
                return island.getName() != null ? island.getName() : nameOf(island);
            case "rank":
                return island.getRole(player.getUniqueId()).getDisplayName();
            case "visitors":
                return island.isLocked()
                        ? plugin.getMessages().get("visit-closed")
                        : plugin.getMessages().get("visit-open");
            case "locked":
                return bool(plugin, island.isLocked());
            case "has_warp":
                return bool(plugin, island.hasWarp());
            case "server":
                return island.getServerName() != null ? island.getServerName() : "";
            default:
                return null;
        }
    }

    /**
     * Replace every {@code %skyblock_...%} token in {@code text} using the
     * internal resolver. Unknown skyblock tokens and all non-skyblock
     * placeholders are left untouched so PlaceholderAPI can still process them.
     */
    public static String apply(SkyblockPlugin plugin, OfflinePlayer player, String text) {
        if (text == null)
            return "";
        if (text.indexOf('%') < 0)
            return text;

        Matcher matcher = TOKEN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String value = resolve(plugin, player, matcher.group(1));
            matcher.appendReplacement(buffer,
                    Matcher.quoteReplacement(value == null ? matcher.group(0) : value));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String noIsland(SkyblockPlugin plugin, String key) {
        switch (key) {
            case "level":
            case "members":
            case "team_limit":
            case "points":
            case "next_points":
            case "bank":
                return "0";
            case "owner":
            case "name":
            case "rank":
            case "server":
                return "";
            case "visitors":
            case "locked":
            case "has_warp":
                return bool(plugin, false);
            default:
                if (key.startsWith("flag_"))
                    return flagState(plugin, false);
                return "";
        }
    }

    private static String flagState(SkyblockPlugin plugin, boolean value) {
        return value
                ? plugin.getMessages().get("flag-on")
                : plugin.getMessages().get("flag-off");
    }

    private static String bool(SkyblockPlugin plugin, boolean value) {
        return value
                ? plugin.getMessages().get("placeholder-yes")
                : plugin.getMessages().get("placeholder-no");
    }

    private static String nameOf(Island island) {
        String name = Bukkit.getOfflinePlayer(island.getOwner()).getName();
        return name != null ? name : island.getOwner().toString().substring(0, 8);
    }

    private static String formatNumber(double value) {
        if (value == Math.floor(value))
            return String.valueOf((long) value);
        return String.format("%.1f", value);
    }
}
