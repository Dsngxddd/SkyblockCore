package net.cengiz1.skyblock.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class Placeholders {

    private static Boolean available;

    private Placeholders() {
    }

    public static boolean isAvailable() {
        if (available == null)
            available = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        return available;
    }

    public static String apply(OfflinePlayer player, String text) {
        if (text == null)
            return "";
        if (!isAvailable())
            return text;
        return setPlaceholders(player, text);
    }

    private static String setPlaceholders(OfflinePlayer player, String text) {
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
    }
}
