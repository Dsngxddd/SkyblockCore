package net.cengiz1.skyblock.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuHolder implements InventoryHolder {

    private final String menuId;
    private final UUID islandId;
    private final Map<Integer, String> actions = new HashMap<>();
    private Inventory inventory;

    public MenuHolder(String menuId, UUID islandId) {
        this.menuId = menuId;
        this.islandId = islandId;
    }

    public String getMenuId() {
        return menuId;
    }

    public UUID getIslandId() {
        return islandId;
    }

    public Map<Integer, String> getActions() {
        return actions;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
