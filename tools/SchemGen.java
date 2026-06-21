import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class SchemGen {

    static final int DATA_VERSION = 4189;

    static final class Cell { final int x, y, z; final String state; Cell(int x,int y,int z,String s){this.x=x;this.y=y;this.z=z;this.state=s;} }

    final List<Cell> cells = new ArrayList<>();

    void set(int x, int y, int z, String state) { cells.add(new Cell(x, y, z, state)); }

    void buildNether() {
        for (int dx = -6; dx <= 6; dx++)
            for (int dz = -6; dz <= 6; dz++)
                if (dx*dx + dz*dz <= 36) {
                    set(dx, 0, dz, "minecraft:netherrack");
                    set(dx, -1, dz, "minecraft:netherrack");
                }
        set(0, -2, 0, "minecraft:bedrock");
        for (int dx = 2; dx <= 3; dx++)
            for (int dz = -1; dz <= 0; dz++) {
                set(dx, 0, dz, "minecraft:soul_sand");
                set(dx, 1, dz, "minecraft:nether_wart[age=0]");
            }
        set(-3, 0, 2, "minecraft:magma_block");
        set(-3, 0, -3, "minecraft:glowstone");
        set(3, 0, 3, "minecraft:glowstone");
        netherPortal(-1, 0, -6);
        endPortalPad(0, 0, 5);
    }

    void netherPortal(int x0, int y0, int z) {
        for (int dx = 0; dx <= 3; dx++) {
            set(x0 + dx, y0, z, "minecraft:obsidian");
            set(x0 + dx, y0 + 4, z, "minecraft:obsidian");
        }
        for (int dy = 0; dy <= 4; dy++) {
            set(x0, y0 + dy, z, "minecraft:obsidian");
            set(x0 + 3, y0 + dy, z, "minecraft:obsidian");
        }
        for (int dx = 1; dx <= 2; dx++)
            for (int dy = 1; dy <= 3; dy++)
                set(x0 + dx, y0 + dy, z, "minecraft:nether_portal[axis=x]");
    }

    void endPortalPad(int cx, int cy, int cz) {
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                set(cx + dx, cy - 1, cz + dz, "minecraft:nether_bricks");
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                set(cx + dx, cy, cz + dz, "minecraft:end_portal");
        frame(cx, cy, cz + 2, "north");
        frame(cx - 1, cy, cz + 2, "north");
        frame(cx + 1, cy, cz + 2, "north");
        frame(cx, cy, cz - 2, "south");
        frame(cx - 1, cy, cz - 2, "south");
        frame(cx + 1, cy, cz - 2, "south");
        frame(cx + 2, cy, cz, "west");
        frame(cx + 2, cy, cz - 1, "west");
        frame(cx + 2, cy, cz + 1, "west");
        frame(cx - 2, cy, cz, "east");
        frame(cx - 2, cy, cz - 1, "east");
        frame(cx - 2, cy, cz + 1, "east");
        for (int dx = -2; dx <= 2; dx += 4)
            for (int dz = -2; dz <= 2; dz += 4)
                set(cx + dx, cy, cz + dz, "minecraft:end_stone_bricks");
    }

    void frame(int x, int y, int z, String facing) {
        set(x, y, z, "minecraft:end_portal_frame[eye=true,facing=" + facing + "]");
    }

    void buildEnd() {
        layer(0, 0, 7, "minecraft:end_stone");
        layer(-1, 0, 6, "minecraft:end_stone");
        layer(-2, 0, 4, "minecraft:end_stone");
        layer(-3, 0, 2, "minecraft:end_stone");
        for (int dy = 1; dy <= 6; dy++)
            set(0, dy, 0, "minecraft:obsidian");
        set(0, 7, 0, "minecraft:end_rod");
        int[][] pillars = {{5, 0}, {-5, 0}, {0, 5}, {0, -5}};
        for (int[] p : pillars) {
            for (int dy = 1; dy <= 3; dy++)
                set(p[0], dy, p[1], "minecraft:purpur_pillar[axis=y]");
            set(p[0], 4, p[1], "minecraft:end_rod");
        }
        set(3, 1, 3, "minecraft:chorus_flower[age=0]");
        set(-3, 1, -2, "minecraft:chorus_flower[age=0]");
        set(2, 1, -4, "minecraft:chorus_flower[age=0]");
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = 0; dz <= 2; dz++)
                set(4 + dx, 1, -6 + dz, "minecraft:purpur_block");
        set(4, 2, -6, "minecraft:end_rod");
        set(4, 2, -5, "minecraft:chest[facing=north,type=single,waterlogged=false]");
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                set(dx, 0, -6 + dz, "minecraft:bedrock");
        set(0, 1, -6, "minecraft:end_portal");
        set(-1, 1, -6, "minecraft:bedrock");
        set(1, 1, -6, "minecraft:bedrock");
        set(0, 1, -7, "minecraft:bedrock");
        set(0, 1, -5, "minecraft:bedrock");
    }

    void layer(int y, int unusedCz, int radius, String state) {
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                if (dx*dx + dz*dz <= r2)
                    set(dx, y, dz, state);
    }

    void write(File out) throws IOException {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Cell c : cells) {
            minX = Math.min(minX, c.x); maxX = Math.max(maxX, c.x);
            minY = Math.min(minY, c.y); maxY = Math.max(maxY, c.y);
            minZ = Math.min(minZ, c.z); maxZ = Math.max(maxZ, c.z);
        }
        int width = maxX - minX + 1, height = maxY - minY + 1, length = maxZ - minZ + 1;

        LinkedHashMap<String, Integer> palette = new LinkedHashMap<>();
        palette.put("minecraft:air", 0);
        for (Cell c : cells) palette.computeIfAbsent(c.state, k -> palette.size());

        int[] data = new int[width * height * length];
        for (Cell c : cells) {
            int lx = c.x - minX, ly = c.y - minY, lz = c.z - minZ;
            int index = (ly * length + lz) * width + lx;
            data[index] = palette.get(c.state);
        }

        ByteArrayOutputStream blockData = new ByteArrayOutputStream();
        for (int v : data) writeVarInt(blockData, v);

        try (DataOutputStream o = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(out)))) {
            o.writeByte(10); o.writeUTF("Schematic");

            intTag(o, "Version", 2);
            intTag(o, "DataVersion", DATA_VERSION);
            shortTag(o, "Width", width);
            shortTag(o, "Height", height);
            shortTag(o, "Length", length);
            intTag(o, "PaletteMax", palette.size());

            o.writeByte(10); o.writeUTF("Palette");
            for (Map.Entry<String, Integer> e : palette.entrySet())
                intTag(o, e.getKey(), e.getValue());
            o.writeByte(0);

            byte[] bd = blockData.toByteArray();
            o.writeByte(7); o.writeUTF("BlockData");
            o.writeInt(bd.length); o.write(bd);

            o.writeByte(9); o.writeUTF("BlockEntities");
            o.writeByte(10); o.writeInt(0);

            o.writeByte(0);
        }

        System.out.printf("%s: %dx%dx%d, palette=%d, localCentre=(%d,%d,%d)%n",
                out.getName(), width, height, length, palette.size(),
                -minX, -minY, -minZ);
    }

    static void intTag(DataOutputStream o, String name, int v) throws IOException {
        o.writeByte(3); o.writeUTF(name); o.writeInt(v);
    }
    static void shortTag(DataOutputStream o, String name, int v) throws IOException {
        o.writeByte(2); o.writeUTF(name); o.writeShort(v);
    }
    static void writeVarInt(OutputStream o, int value) throws IOException {
        while ((value & ~0x7F) != 0) { o.write((value & 0x7F) | 0x80); value >>>= 7; }
        o.write(value & 0x7F);
    }

    public static void main(String[] args) throws IOException {
        File dir = new File(args.length > 0 ? args[0] : ".");
        dir.mkdirs();

        SchemGen nether = new SchemGen();
        nether.buildNether();
        nether.write(new File(dir, "nether.schem"));

        SchemGen end = new SchemGen();
        end.buildEnd();
        end.write(new File(dir, "end.schem"));
    }
}
