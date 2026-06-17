package net.cengiz1.skyblock.schematic;

import org.bukkit.World;

import java.util.Collection;

public interface SchematicService {

    boolean isReady();

    /** True when the schematic engine (FastAsyncWorldEdit) is present, regardless of configured island schematics. */
    boolean isAvailable();

    /** True when a raw schematic file with this name exists in the schematics folder. */
    boolean hasFile(String fileName);

    /**
     * Pastes a raw schematic file (by file name) at the given coordinates. Used by
     * non-island plots such as the per-island Nether/End starters. Returns false
     * if the engine is unavailable, the file is missing or the paste failed.
     */
    boolean pasteFile(World world, int x, int y, int z, String fileName);

    boolean has(String key);

    String getDefaultKey();

    Collection<SchematicDefinition> getDefinitions();

    SchematicDefinition get(String key);

    void paste(SchematicDefinition definition, World world, int x, int y, int z) throws Exception;

    boolean clearRegion(World world, int centerX, int centerZ, int half);

    void reload();
}
