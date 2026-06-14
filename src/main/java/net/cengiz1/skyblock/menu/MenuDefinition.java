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
        // Bu öğenin lore'una block-values.yml'deki blok değerleri eklensin mi?
        public final boolean blockValues;

        public Entry(int slot, Material material, int amount, String name,
                     List<String> lore, String action, boolean blockValues) {
            this.slot = slot;
            this.material = material;
            this.amount = amount;
            this.name = name;
            this.lore = lore;
            this.action = action;
            this.blockValues = blockValues;
        }
    }

    private final String title;
    private final int rows;
    private final String type; // "normal" | "upgrades"
    private final List<Entry> entries = new ArrayList<>();

    // Yükseltme menüsü için lore şablonları
    private List<String> upgradeLore = new ArrayList<>();
    private List<String> upgradeLoreMax = new ArrayList<>();
    // Blok değerleri lore formatı (örn: "&7• &f{block}: &e{value}")
    private String blockValuesFormat = "&7• &f{block}: &e{value}";

    public MenuDefinition(String title, int rows, String type) {
        this.title = title;
        this.rows = rows;
        this.type = type == null ? "normal" : type.toLowerCase();
    }

    public String getTitle() {
        return title;
    }

    public int getRows() {
        return rows;
    }

    public String getType() {
        return type;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public List<String> getUpgradeLore() {
        return upgradeLore;
    }

    public void setUpgradeLore(List<String> upgradeLore) {
        this.upgradeLore = upgradeLore;
    }

    public List<String> getUpgradeLoreMax() {
        return upgradeLoreMax;
    }

    public void setUpgradeLoreMax(List<String> upgradeLoreMax) {
        this.upgradeLoreMax = upgradeLoreMax;
    }

    public String getBlockValuesFormat() {
        return blockValuesFormat;
    }

    public void setBlockValuesFormat(String blockValuesFormat) {
        if (blockValuesFormat != null)
            this.blockValuesFormat = blockValuesFormat;
    }
}
