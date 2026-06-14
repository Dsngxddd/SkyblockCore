package net.cengiz1.skyblock.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomyHook implements EconomyHook {

    private final Economy economy;

    private VaultEconomyHook(Economy economy) {
        this.economy = economy;
    }

    public static EconomyHook setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null)
            return null;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return null;
        Economy economy = rsp.getProvider();
        if (economy == null)
            return null;
        return new VaultEconomyHook(economy);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean has(Player player, double amount) {
        return amount <= 0 || this.economy.has(player, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (amount <= 0)
            return true;
        return this.economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public double balance(Player player) {
        return this.economy.getBalance(player);
    }
}
