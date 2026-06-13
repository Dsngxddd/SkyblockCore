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
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
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

                this.definitions.put(key.toLowerCase(),
                        new SchematicDefinition(key.toLowerCase(), displayName, file, offsetX, offsetY, offsetZ));
            }
        }

        if (this.defaultKey == null && !this.definitions.isEmpty())
            this.defaultKey = this.definitions.keySet().iterator().next();
        else if (this.defaultKey != null)
            this.defaultKey = this.defaultKey.toLowerCase();

        if (!this.available)
            plugin.getLogger().warning("FastAsyncWorldEdit not found. Islands will use the fallback platform.");
    }

    @Override
    public boolean isReady() {
        return this.available && !this.definitions.isEmpty();
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

    private Clipboard getClipboard(SchematicDefinition definition) throws IOException {
        Clipboard cached = this.clipboards.get(definition.getKey());
        if (cached != null)
            return cached;

        File file = new File(this.schematicsFolder, definition.getFileName());
        if (!file.exists())
            throw new FileNotFoundException("Schematic file not found: " + file.getName());

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null)
            throw new IOException("Unknown schematic format: " + file.getName());

        try (ClipboardReader reader = format.getReader(Files.newInputStream(file.toPath()))) {
            Clipboard clipboard = reader.read();
            this.clipboards.put(definition.getKey(), clipboard);
            return clipboard;
        }
    }
}
