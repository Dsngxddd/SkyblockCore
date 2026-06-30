package net.cengiz1.uxmskyblock.island;

import net.cengiz1.uxmskyblock.UxmSkyblockPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the island leaderboard, ordered by level then points. Works for every
 * island in the cache, so it functions the same whether the world is a classic
 * skyblock world or a Chunklock-style natural world.
 */
public class TopService {

    private final UxmSkyblockPlugin plugin;

    private List<Island> cached;
    private long cachedAt;
    private static final long CACHE_MS = 3000;

    public TopService(UxmSkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    private List<Island> sorted() {
        long now = System.currentTimeMillis();
        if (cached != null && now - cachedAt < CACHE_MS)
            return cached;
        List<Island> all = new ArrayList<>(plugin.getIslandManager().getAllIslands());
        all.sort((a, b) -> {
            int byLevel = Integer.compare(b.getLevel(), a.getLevel());
            return byLevel != 0 ? byLevel : Double.compare(b.getPoints(), a.getPoints());
        });
        cached = all;
        cachedAt = now;
        return all;
    }

    public void invalidate() {
        this.cached = null;
    }

    public List<Island> getTop(int limit) {
        List<Island> all = sorted();
        if (limit > 0 && all.size() > limit)
            return new ArrayList<>(all.subList(0, limit));
        return new ArrayList<>(all);
    }

    public Island getAt(int position) {
        if (position < 1)
            return null;
        List<Island> all = sorted();
        return position <= all.size() ? all.get(position - 1) : null;
    }

    /** 1-based rank of an island in the leaderboard, or -1 if it has none. */
    public int getRank(Island island) {
        if (island == null)
            return -1;
        List<Island> all = sorted();
        for (int i = 0; i < all.size(); i++)
            if (all.get(i).getUniqueId().equals(island.getUniqueId()))
                return i + 1;
        return -1;
    }
}
