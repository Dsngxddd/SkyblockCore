package net.cengiz1.skyblock.island;

import net.cengiz1.skyblock.SkyblockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BorderManager implements Listener {

    private final SkyblockPlugin plugin;
    private final IslandManager islandManager;

    private final Map<UUID, UUID> lastIsland = new ConcurrentHashMap<>();

    public BorderManager(SkyblockPlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        update(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.lastIsland.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null)
            return;

        Bukkit.getScheduler().runTask(plugin, () -> update(player, player.getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null)
            return;

        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ())
            return;
        update(event.getPlayer(), to);
    }

    public void update(Player player, Location location) {
        if (!plugin.getSettings().isBorderEnabled())
            return;

        Island island = this.islandManager.getIslandAt(location);
        UUID newId = island == null ? null : island.getUniqueId();
        UUID oldId = this.lastIsland.get(player.getUniqueId());
        if (Objects.equals(oldId, newId))
            return;

        if (newId == null) {
            this.lastIsland.remove(player.getUniqueId());
            player.setWorldBorder(null);
        } else {
            this.lastIsland.put(player.getUniqueId(), newId);
            apply(player, island);
        }
    }

    public void refresh(Island island) {
        if (!plugin.getSettings().isBorderEnabled())
            return;
        UUID islandId = island.getUniqueId();
        for (Map.Entry<UUID, UUID> entry : this.lastIsland.entrySet()) {
            if (!islandId.equals(entry.getValue()))
                continue;
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline())
                apply(player, island);
        }
    }

    public void apply(Player player, Island island) {
        if (!plugin.getSettings().isBorderEnabled())
            return;
        this.lastIsland.put(player.getUniqueId(), island.getUniqueId());

        double size = this.islandManager.getProtectionHalf(island) * 2.0 + 1.0;

        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(island.getCenterX() + 0.5, island.getCenterZ() + 0.5);
        border.setSize(size);
        border.setWarningDistance(0);
        border.setWarningTime(0);

        switch (plugin.getSettings().getBorderColor()) {
            case "GREEN":
                border.setSize(size + 10.0, 9_999_999L);
                break;
            case "RED":
                border.setSize(Math.max(3.0, size - 10.0), 9_999_999L);
                break;
            default:
                break;
        }

        player.setWorldBorder(border);
    }
}
