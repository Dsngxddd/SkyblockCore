package net.cengiz1.skyblock.command;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.economy.EconomyHook;
import net.cengiz1.skyblock.island.Island;
import net.cengiz1.skyblock.island.IslandPermission;
import org.bukkit.entity.Player;

/**
 * Island bank: a shared balance stored on the island. Members deposit from their
 * own Vault wallet into the bank; members with the BANK permission can withdraw
 * back into their wallet. Requires a Vault economy (economy.enabled).
 */
public class BankCommands extends CommandHandler {

    public BankCommands(SkyblockPlugin plugin) {
        super(plugin);
    }

    public void handle(Player player, String action, String amountArg) {
        Island island = plugin.getIslandManager().getByMember(player.getUniqueId());
        if (island == null) {
            plugin.getMessages().send(player, "no-island");
            return;
        }

        if (action == null || action.equalsIgnoreCase("balance")
                || action.equalsIgnoreCase("bakiye") || action.equalsIgnoreCase("info")) {
            plugin.getMessages().send(player, "bank-balance", "{amount}", formatNumber(island.getBank()));
            return;
        }

        EconomyHook economy = plugin.getEconomy();
        if (economy == null || !economy.isEnabled()) {
            plugin.getMessages().send(player, "bank-no-economy");
            return;
        }

        boolean deposit = action.equalsIgnoreCase("deposit")
                || action.equalsIgnoreCase("yatir") || action.equalsIgnoreCase("yatır");
        boolean withdraw = action.equalsIgnoreCase("withdraw")
                || action.equalsIgnoreCase("cek") || action.equalsIgnoreCase("çek");

        if (!deposit && !withdraw) {
            plugin.getMessages().send(player, "bank-usage");
            return;
        }

        double amount = parseAmount(amountArg);
        if (amount <= 0) {
            plugin.getMessages().send(player, "bank-invalid-amount");
            return;
        }

        if (deposit) {
            if (!economy.has(player, amount)) {
                plugin.getMessages().send(player, "bank-need-money");
                return;
            }
            if (!economy.withdraw(player, amount)) {
                plugin.getMessages().send(player, "bank-need-money");
                return;
            }
            island.depositBank(amount);
            plugin.getIslandManager().saveAsync(island);
            plugin.getMessages().send(player, "bank-deposit",
                    "{amount}", formatNumber(amount), "{balance}", formatNumber(island.getBank()));
            return;
        }
        if (!island.hasPermission(player.getUniqueId(), IslandPermission.BANK)) {
            plugin.getMessages().send(player, "no-island-permission");
            return;
        }
        if (island.getBank() < amount) {
            plugin.getMessages().send(player, "bank-insufficient", "{amount}", formatNumber(island.getBank()));
            return;
        }
        double taken = island.withdrawBank(amount);
        if (!economy.deposit(player, taken)) {
            island.depositBank(taken);
            plugin.getMessages().send(player, "bank-failed");
            return;
        }
        plugin.getIslandManager().saveAsync(island);
        plugin.getMessages().send(player, "bank-withdraw",
                "{amount}", formatNumber(taken), "{balance}", formatNumber(island.getBank()));
    }

    private double parseAmount(String arg) {
        if (arg == null)
            return -1;
        try {
            return Double.parseDouble(arg.trim().replace(",", "."));
        } catch (NumberFormatException error) {
            return -1;
        }
    }
}
