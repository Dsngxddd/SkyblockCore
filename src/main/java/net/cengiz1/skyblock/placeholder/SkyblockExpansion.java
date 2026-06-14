package net.cengiz1.skyblock.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandFlag;
import org.bukkit.OfflinePlayer;

public class SkyblockExpansion extends PlaceholderExpansion {

    private final SkyblockPlugin plugin;

    public SkyblockExpansion(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "skyblock";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty()
                ? "cengiz1x" : plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null)
            return "";

        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        String key = params.toLowerCase();

        if (key.equals("has_island"))
            return bool(island != null);

        if (island == null)
            return noIsland(key);

        if (key.startsWith("flag_")) {
            IslandFlag flag = IslandFlag.fromString(key.substring("flag_".length()));
            if (flag == null)
                return null;
            return flagState(island.getFlag(flag));
        }

        switch (key) {
            case "level":
                return String.valueOf(island.getLevel());
            case "points":
                return formatNumber(island.getPoints());
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
                return bool(island.isLocked());
            case "has_warp":
                return bool(island.hasWarp());
            case "server":
                return island.getServerName() != null ? island.getServerName() : "";
            default:
                return null;
        }
    }

    private String noIsland(String key) {
        switch (key) {
            case "level":
            case "members":
            case "team_limit":
                return "0";
            case "points":
            case "next_points":
                return "0";
            case "owner":
            case "name":
            case "rank":
            case "server":
                return "";
            case "visitors":
            case "locked":
            case "has_warp":
                return bool(false);
            default:
                if (key.startsWith("flag_"))
                    return flagState(false);
                return "";
        }
    }

    private String flagState(boolean value) {
        return value
                ? plugin.getMessages().get("flag-on")
                : plugin.getMessages().get("flag-off");
    }

    private String bool(boolean value) {
        return value
                ? plugin.getMessages().get("placeholder-yes")
                : plugin.getMessages().get("placeholder-no");
    }

    private String nameOf(Island island) {
        String name = org.bukkit.Bukkit.getOfflinePlayer(island.getOwner()).getName();
        return name != null ? name : island.getOwner().toString().substring(0, 8);
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value))
            return String.valueOf((long) value);
        return String.format("%.1f", value);
    }
}
