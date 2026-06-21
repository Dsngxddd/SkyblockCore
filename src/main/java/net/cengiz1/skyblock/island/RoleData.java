package net.cengiz1.skyblock.island;

import java.util.EnumSet;
import java.util.Set;

/**
 * A role on an island. Built-in roles (visitor/member/owner ...) come from
 * roles.yml and are shared by every island; custom roles are created per island
 * by the owner. A role is identified by its lowercase {@code id}.
 */
public class RoleData {

    private final String id;
    private String displayName;
    private int weight;
    private final EnumSet<IslandPermission> permissions;
    private final boolean builtin;

    public RoleData(String id, String displayName, int weight, boolean builtin) {
        this.id = id.toLowerCase(java.util.Locale.ROOT);
        this.displayName = displayName;
        this.weight = weight;
        this.builtin = builtin;
        this.permissions = EnumSet.noneOf(IslandPermission.class);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : id;
    }

    public void setDisplayName(String displayName) {
        if (displayName != null && !displayName.isEmpty())
            this.displayName = displayName;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public Set<IslandPermission> getPermissions() {
        return permissions;
    }

    public boolean has(IslandPermission permission) {
        return permissions.contains(permission);
    }

    public void setPermission(IslandPermission permission, boolean value) {
        if (value)
            permissions.add(permission);
        else
            permissions.remove(permission);
    }

    public void setPermissions(Set<IslandPermission> values) {
        permissions.clear();
        permissions.addAll(values);
    }

    public boolean canManage(RoleData target) {
        return target == null || this.weight > target.weight;
    }

    public RoleData copy() {
        RoleData copy = new RoleData(id, displayName, weight, builtin);
        copy.permissions.addAll(this.permissions);
        return copy;
    }
}
