package net.cengiz1.skyblock.island;

import net.cengiz1.skyblock.SkyblockPlugin;
import net.cengiz1.skyblock.config.SettingsManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Ada zamanı sabitlenmişse, ada içindeki oyunculara o saati gösterir;
 * ada dışına çıkınca normal döngüye döner.
 */
public class IslandTimeTask extends BukkitRunnable {

    private final SkyblockPlugin plugin;
    private final IslandManager islandManager;
    private final String worldName;

    public IslandTimeTask(SkyblockPlugin plugin, IslandManager islandManager, SettingsManager settings) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.worldName = settings.getWorldName();
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getWorld() == null || !player.getWorld().getName().equals(this.worldName)) {
                continue;
            }
            Island island = this.islandManager.getIslandAt(player.getLocation());
            if (island != null && island.getTime().isFixed()) {
                player.setPlayerTime(island.getTime().getTicks(), false);
            } else {
                player.resetPlayerTime();
            }
        }
    }
}
