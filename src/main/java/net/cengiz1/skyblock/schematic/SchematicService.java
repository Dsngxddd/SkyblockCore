package net.cengiz1.skyblock.schematic;

import org.bukkit.World;

import java.util.Collection;

public interface SchematicService {

    boolean isReady();

    boolean has(String key);

    String getDefaultKey();

    Collection<SchematicDefinition> getDefinitions();

    SchematicDefinition get(String key);

    void paste(SchematicDefinition definition, World world, int x, int y, int z) throws Exception;

    /**
     * Merkezi (centerX, centerZ) olan, yatayda ±half yarıçaplı ve dikeyde tüm
     * dünya yüksekliği boyunca uzanan bölgeyi havaya (siler) çevirir.
     * FAWE yoksa false döner (çağıran yedek temizliğe geçmeli).
     */
    boolean clearRegion(World world, int centerX, int centerZ, int half);

    void reload();
}
