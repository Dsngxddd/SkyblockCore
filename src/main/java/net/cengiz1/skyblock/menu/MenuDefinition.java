package net.cengiz1.skyblock.menu;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class MenuDefinition {

    public static class Entry {
        public final int slot;
        public final Material material;
        public final int amount;
        public final String name;
        public final List<String> lore;
        public final String action;

        public Entry(int slot, Material material, int amount, String name, List<String> lore, String action) {
            this.slot = slot;
            this.material = material;
            this.amount = amount;
            this.name = name;
            this.lore = lore;
            this.action = action;
        }
    }

    private final String title;
    private final int rows;
    private final List<Entry> entries = new ArrayList<>();

    public MenuDefinition(String title, int rows) {
        this.title = title;
        this.rows = rows;
    }

    public String getTitle() {
        return title;
    }

    public int getRows() {
        return rows;
    }

    public List<Entry> getEntries() {
        return entries;
    }
}
