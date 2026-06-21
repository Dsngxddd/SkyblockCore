package net.cengiz1.skyblock.schematic;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.cengiz1.skyblock.SkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FaweSchematicService implements SchematicService {

    private final SkyblockPlugin plugin;
    private final File schematicsFolder;

    private boolean available;
    private String defaultKey;
    private final Map<String, SchematicDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, Clipboard> clipboards = new ConcurrentHashMap<>();

    public FaweSchematicService(SkyblockPlugin plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        reload();
    }

    @Override
    public void reload() {
        this.definitions.clear();
        this.clipboards.clear();

        if (!this.schematicsFolder.exists())
            this.schematicsFolder.mkdirs();

        this.available = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("schematics");
        if (root == null) {
            plugin.getLogger().warning("No schematics section in config.");
            return;
        }

        this.defaultKey = root.getString("default", null);

        ConfigurationSection list = root.getConfigurationSection("list");
        if (list != null) {
            for (String key : list.getKeys(false)) {
                ConfigurationSection entry = list.getConfigurationSection(key);
                if (entry == null)
                    continue;

                String displayName = entry.getString("display-name", key);
                String file = entry.getString("file", key + ".schem");
                double offsetX = entry.getDouble("home-offset.x", 0.5);
                double offsetY = entry.getDouble("home-offset.y", 1.0);
                double offsetZ = entry.getDouble("home-offset.z", 0.5);

                extractBundled(file);
                this.definitions.put(key.toLowerCase(),
                        new SchematicDefinition(key.toLowerCase(), displayName, file, offsetX, offsetY, offsetZ));
            }
        }

        extractBundled(plugin.getConfig().getString("nether.schematic", ""));
        extractBundled(plugin.getConfig().getString("end.schematic", ""));

        if (this.defaultKey == null && !this.definitions.isEmpty())
            this.defaultKey = this.definitions.keySet().iterator().next();
        else if (this.defaultKey != null)
            this.defaultKey = this.defaultKey.toLowerCase();

        if (!this.available)
            plugin.getLogger().warning("FastAsyncWorldEdit not found. Islands will use the fallback platform.");
    }

    private void extractBundled(String fileName) {
        if (fileName == null || fileName.isBlank())
            return;
        File target = new File(this.schematicsFolder, fileName);
        if (target.exists())
            return;
        if (plugin.getResource("schematics/" + fileName) == null)
            return;
        plugin.saveResource("schematics/" + fileName, false);
    }

    @Override
    public boolean isReady() {
        return this.available && !this.definitions.isEmpty();
    }

    @Override
    public boolean isAvailable() {
        return this.available;
    }

    @Override
    public boolean hasFile(String fileName) {
        if (fileName == null || fileName.isBlank())
            return false;
        return new File(this.schematicsFolder, fileName).exists();
    }

    @Override
    public boolean pasteFile(World bukkitWorld, int x, int y, int z, String fileName) {
        if (!this.available || fileName == null || fileName.isBlank())
            return false;

        File file = new File(this.schematicsFolder, fileName);
        if (!file.exists()) {
            plugin.getLogger().warning("Schematic file not found: " + file.getName());
            return false;
        }

        try {
            Clipboard clipboard = loadClipboard("file:" + fileName.toLowerCase(), file);
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
            try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(session)
                        .to(BlockVector3.at(x, y, z))
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
            }
            return true;
        } catch (Throwable error) {
            plugin.getLogger().warning("Schematic paste failed (" + fileName + "): " + error.getMessage());
            return false;
        }
    }

    @Override
    public boolean has(String key) {
        return key != null && this.definitions.containsKey(key.toLowerCase());
    }

    @Override
    public String getDefaultKey() {
        return this.defaultKey;
    }

    @Override
    public Collection<SchematicDefinition> getDefinitions() {
        return this.definitions.values();
    }

    @Override
    public SchematicDefinition get(String key) {
        return key == null ? null : this.definitions.get(key.toLowerCase());
    }

    @Override
    public void paste(SchematicDefinition definition, World bukkitWorld, int x, int y, int z) throws Exception {
        Clipboard clipboard = getClipboard(definition);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);

        try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(session)
                    .to(BlockVector3.at(x, y, z))
                    .ignoreAirBlocks(false)
                    .build();
            Operations.complete(operation);
        }
    }

    @Override
    public boolean clearRegion(World bukkitWorld, int centerX, int centerZ, int half) {
        if (!this.available)
            return false;

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        int minY = bukkitWorld.getMinHeight();
        int maxY = bukkitWorld.getMaxHeight() - 1;

        BlockVector3 min = BlockVector3.at(centerX - half, minY, centerZ - half);
        BlockVector3 max = BlockVector3.at(centerX + half, maxY, centerZ + half);

        try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {
            Region region = new CuboidRegion(weWorld, min, max);
            session.setBlocks(region, (Pattern) BlockTypes.AIR.getDefaultState());
            return true;
        } catch (Throwable error) {
            plugin.getLogger().warning("Could not clear island region: " + error.getMessage());
            return false;
        }
    }

    private Clipboard getClipboard(SchematicDefinition definition) throws IOException {
        File file = new File(this.schematicsFolder, definition.getFileName());
        return loadClipboard(definition.getKey(), file);
    }

    private Clipboard loadClipboard(String cacheKey, File file) throws IOException {
        Clipboard cached = this.clipboards.get(cacheKey);
        if (cached != null)
            return cached;

        if (!file.exists())
            throw new FileNotFoundException("Schematic file not found: " + file.getName());

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null)
            throw new IOException("Unknown schematic format: " + file.getName());

        try (ClipboardReader reader = format.getReader(Files.newInputStream(file.toPath()))) {
            Clipboard clipboard = reader.read();
            this.clipboards.put(cacheKey, clipboard);
            return clipboard;
        }
    }
}
