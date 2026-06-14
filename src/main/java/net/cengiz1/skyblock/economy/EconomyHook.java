package net.cengiz1.skyblock.economy;

import org.bukkit.entity.Player;

public interface EconomyHook {

    boolean isEnabled();

    boolean has(Player player, double amount);

    boolean withdraw(Player player, double amount);

    double balance(Player player);
}
