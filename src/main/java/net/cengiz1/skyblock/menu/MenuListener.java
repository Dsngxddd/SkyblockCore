package net.cengiz1.skyblock.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class MenuListener implements Listener {

    private final MenuManager menuManager;

    public MenuListener(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder))
            return;

        event.setCancelled(true);

        if (event.getClickedInventory() != top)
            return;
        if (!(event.getWhoClicked() instanceof Player))
            return;

        MenuHolder holder = (MenuHolder) top.getHolder();
        String action = holder.getActions().get(event.getRawSlot());
        if (action == null)
            return;

        menuManager.handleAction((Player) event.getWhoClicked(), holder, action);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder)
            event.setCancelled(true);
    }
}
