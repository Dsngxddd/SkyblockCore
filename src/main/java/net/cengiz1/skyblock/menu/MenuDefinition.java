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
    private final String type;
    private final List<Entry> entries = new ArrayList<>();

    private List<String> upgradeLore = new ArrayList<>();
    private List<String> upgradeLoreMax = new ArrayList<>();
    private String blockValuesFormat = "&7• &f{block}: &e{value}";

    private List<Integer> headSlots = new ArrayList<>();
    private String headName = "&f{owner} &7[{level}]";
    private List<String> headLore = new ArrayList<>();
    private String memberFormat = "&8• &f{player} &7{role}";

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

    public List<Integer> getHeadSlots() {
        return headSlots;
    }

    public void setHeadSlots(List<Integer> headSlots) {
        if (headSlots != null)
            this.headSlots = headSlots;
    }

    public String getHeadName() {
        return headName;
    }

    public void setHeadName(String headName) {
        if (headName != null)
            this.headName = headName;
    }

    public List<String> getHeadLore() {
        return headLore;
    }

    public void setHeadLore(List<String> headLore) {
        if (headLore != null)
            this.headLore = headLore;
    }

    public String getMemberFormat() {
        return memberFormat;
    }

    public void setMemberFormat(String memberFormat) {
        if (memberFormat != null)
            this.memberFormat = memberFormat;
    }
}
