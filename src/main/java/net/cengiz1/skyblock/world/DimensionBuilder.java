package net.cengiz1.skyblock.world;

import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.inventory.ItemStack;

/**
 * Builds the per-island Nether and End starter plots block-by-block (no
 * schematic file needed, so it works without FastAsyncWorldEdit). Each build is
 * centred on the island's grid coordinates; the floor sits at {@code cy}, the
 * player stands at {@code cy + 1}.
 *
 * Layout per dimension:
 *  - Nether: a netherrack platform with a soul-sand / nether-wart starter farm,
 *    a lit return portal back to the overworld, and a ready end portal to dive
 *    into the island's End plot.
 *  - End: a floating end-stone island crowned by an obsidian spire and purpur
 *    pillars (end-rod lit), chorus plants, a small loot ship, and a return
 *    portal home.
 */
public final class DimensionBuilder {

    private DimensionBuilder() {
    }
    public static void buildNether(World world, int cx, int cy, int cz) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (dx * dx + dz * dz <= 36) {
                    set(world, cx + dx, cy, cz + dz, Material.NETHERRACK);
                    set(world, cx + dx, cy - 1, cz + dz, Material.NETHERRACK);
                }
            }
        }
        set(world, cx, cy - 2, cz, Material.BEDROCK);
        for (int dx = 2; dx <= 3; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                set(world, cx + dx, cy, cz + dz, Material.SOUL_SAND);
                set(world, cx + dx, cy + 1, cz + dz, Material.NETHER_WART);
            }
        }
        set(world, cx - 3, cy, cz + 2, Material.MAGMA_BLOCK);
        set(world, cx - 3, cy, cz - 3, Material.GLOWSTONE);
        set(world, cx + 3, cy, cz + 3, Material.GLOWSTONE);
        buildNetherPortal(world, cx - 1, cy, cz - 6);
        buildEndPortalPad(world, cx, cy, cz + 5);
    }
    private static void buildNetherPortal(World world, int x0, int y0, int z) {
        for (int dx = 0; dx <= 3; dx++) {
            set(world, x0 + dx, y0, z, Material.OBSIDIAN);
            set(world, x0 + dx, y0 + 4, z, Material.OBSIDIAN);
        }
        for (int dy = 0; dy <= 4; dy++) {
            set(world, x0, y0 + dy, z, Material.OBSIDIAN);
            set(world, x0 + 3, y0 + dy, z, Material.OBSIDIAN);
        }
        BlockData portal = org.bukkit.Bukkit.createBlockData(Material.NETHER_PORTAL);
        if (portal instanceof Orientable orientable)
            orientable.setAxis(Axis.X);
        for (int dx = 1; dx <= 2; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                Block block = world.getBlockAt(x0 + dx, y0 + dy, z);
                block.setBlockData(portal, false);
            }
        }
    }
    private static void buildEndPortalPad(World world, int cx, int cy, int cz) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                set(world, cx + dx, cy - 1, cz + dz, Material.NETHER_BRICKS);
            }
        }
        fillEndPortal(world, cx, cy, cz);
    }

    private static void fillEndPortal(World world, int cx, int cy, int cz) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                set(world, cx + dx, cy, cz + dz, Material.END_PORTAL);

        frame(world, cx, cy, cz + 2, BlockFace.NORTH);
        frame(world, cx - 1, cy, cz + 2, BlockFace.NORTH);
        frame(world, cx + 1, cy, cz + 2, BlockFace.NORTH);
        frame(world, cx, cy, cz - 2, BlockFace.SOUTH);
        frame(world, cx - 1, cy, cz - 2, BlockFace.SOUTH);
        frame(world, cx + 1, cy, cz - 2, BlockFace.SOUTH);
        frame(world, cx + 2, cy, cz, BlockFace.WEST);
        frame(world, cx + 2, cy, cz - 1, BlockFace.WEST);
        frame(world, cx + 2, cy, cz + 1, BlockFace.WEST);
        frame(world, cx - 2, cy, cz, BlockFace.EAST);
        frame(world, cx - 2, cy, cz - 1, BlockFace.EAST);
        frame(world, cx - 2, cy, cz + 1, BlockFace.EAST);

        for (int dx = -2; dx <= 2; dx += 4)
            for (int dz = -2; dz <= 2; dz += 4)
                set(world, cx + dx, cy, cz + dz, Material.END_STONE_BRICKS);
    }

    private static void frame(World world, int x, int y, int z, BlockFace facing) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.END_PORTAL_FRAME, false);
        BlockData data = block.getBlockData();
        if (data instanceof EndPortalFrame endFrame) {
            endFrame.setEye(true);
            endFrame.setFacing(facing);
            block.setBlockData(endFrame, false);
        }
    }
    public static void buildEnd(World world, int cx, int cy, int cz) {
        layer(world, cx, cy, cz, 7, Material.END_STONE);
        layer(world, cx, cy - 1, cz, 6, Material.END_STONE);
        layer(world, cx, cy - 2, cz, 4, Material.END_STONE);
        layer(world, cx, cy - 3, cz, 2, Material.END_STONE);
        for (int dy = 1; dy <= 6; dy++)
            set(world, cx, cy + dy, cz, Material.OBSIDIAN);
        set(world, cx, cy + 7, cz, Material.END_ROD);
        int[][] pillars = {{5, 0}, {-5, 0}, {0, 5}, {0, -5}};
        for (int[] p : pillars) {
            for (int dy = 1; dy <= 3; dy++)
                set(world, cx + p[0], cy + dy, cz + p[1], Material.PURPUR_PILLAR);
            set(world, cx + p[0], cy + 4, cz + p[1], Material.END_ROD);
        }
        set(world, cx + 3, cy + 1, cz + 3, Material.CHORUS_FLOWER);
        set(world, cx - 3, cy + 1, cz - 2, Material.CHORUS_FLOWER);
        set(world, cx + 2, cy + 1, cz - 4, Material.CHORUS_FLOWER);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = 0; dz <= 2; dz++)
                set(world, cx + 4 + dx, cy + 1, cz - 6 + dz, Material.PURPUR_BLOCK);
        set(world, cx + 4, cy + 2, cz - 6, Material.END_ROD);
        placeLootChest(world, cx + 4, cy + 2, cz - 5);
        buildEndReturnPortal(world, cx, cy, cz - 6);
    }
    private static void layer(World world, int cx, int cy, int cz, int radius, Material material) {
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                if (dx * dx + dz * dz <= r2)
                    set(world, cx + dx, cy, cz + dz, material);
    }

    private static void placeLootChest(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.CHEST, false);
        if (block.getState() instanceof Chest chest) {
            chest.getBlockInventory().addItem(
                    new ItemStack(Material.OBSIDIAN, 10),
                    new ItemStack(Material.ENDER_PEARL, 4),
                    new ItemStack(Material.CHORUS_FRUIT, 8));
            chest.update();
        }
    }
    private static void buildEndReturnPortal(World world, int cx, int cy, int cz) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                set(world, cx + dx, cy, cz + dz, Material.BEDROCK);
        set(world, cx, cy + 1, cz, Material.END_PORTAL);
        set(world, cx - 1, cy + 1, cz, Material.BEDROCK);
        set(world, cx + 1, cy + 1, cz, Material.BEDROCK);
        set(world, cx, cy + 1, cz - 1, Material.BEDROCK);
        set(world, cx, cy + 1, cz + 1, Material.BEDROCK);
    }

    private static void set(World world, int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material, false);
    }
}
