package net.cengiz1.skyblock.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuHolder implements InventoryHolder {

    private final String menuId;
    private final UUID islandId;
    private final int page;
    private int pageCount = 1;
    private final Map<Integer, String> actions = new HashMap<>();
    private Inventory inventory;

    public MenuHolder(String menuId, UUID islandId, int page) {
        this.menuId = menuId;
        this.islandId = islandId;
        this.page = page;
    }

    public String getMenuId() {
        return menuId;
    }

    public UUID getIslandId() {
        return islandId;
    }

    public int getPage() {
        return page;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = Math.max(1, pageCount);
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
