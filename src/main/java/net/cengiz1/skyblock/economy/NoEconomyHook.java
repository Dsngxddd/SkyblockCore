package net.cengiz1.skyblock.economy;

import org.bukkit.entity.Player;

public class NoEconomyHook implements EconomyHook {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean has(Player player, double amount) {
        return true;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return true;
    }

    @Override
    public double balance(Player player) {
        return 0;
    }
}
