package net.cengiz1.skyblock.storage;

import net.cengiz1.skyblock.island.Island;

import java.util.Collection;
import java.util.UUID;

public interface Storage {

    void init() throws Exception;

    Collection<Island> loadAll();

    Island load(UUID islandId);

    void save(Island island);

    void delete(UUID islandId);

    void close();
}
