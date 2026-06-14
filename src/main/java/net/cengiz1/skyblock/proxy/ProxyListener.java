package net.cengiz1.skyblock.proxy;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ProxyListener implements Listener {

    private final ProxyManager proxyManager;

    public ProxyListener(ProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        proxyManager.onPlayerJoin(event.getPlayer());
    }
}
