package net.cengiz1.skyblock.listener;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.island.BorderManager;
import net.cengiz1.skyblock.island.IslandManager;
import net.cengiz1.skyblock.level.BlockTrackListener;
import net.cengiz1.skyblock.menu.MenuListener;
import net.cengiz1.skyblock.proxy.ProxyListener;
import net.cengiz1.skyblock.upgrade.UpgradeEffectListener;
import org.bukkit.event.Listener;

public final class ListenerRegistrar {

    private ListenerRegistrar() {
    }

    public static void registerAll(SkyblockPlugin plugin) {
        IslandManager islandManager = plugin.getIslandManager();

        register(plugin, new MenuListener(plugin.getMenuManager()));

        register(plugin, new PvpListener(islandManager));
        register(plugin, new MobSpawnListener(islandManager));
        register(plugin, new ExplosionListener(islandManager));
        register(plugin, new FireListener(islandManager));
        register(plugin, new VisitorInteractListener(islandManager));
        register(plugin, new WarpSignListener(plugin));
        register(plugin, new ObsidianBucketListener(plugin, islandManager));

        if (plugin.getDimensionManager() != null)
            register(plugin, new PortalListener(plugin));

        register(plugin, new BlockTrackListener(plugin, islandManager,
                plugin.getBlockValueManager(), plugin.getLevelManager()));
        register(plugin, new UpgradeEffectListener(islandManager, plugin.getUpgradeManager()));

        BorderManager borderManager = new BorderManager(plugin, islandManager);
        islandManager.setBorderManager(borderManager);
        register(plugin, borderManager);

        if (plugin.getProxyManager() != null && plugin.getProxyManager().isEnabled())
            register(plugin, new ProxyListener(plugin.getProxyManager()));
    }

    private static void register(SkyblockPlugin plugin, Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }
}
