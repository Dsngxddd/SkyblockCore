package net.cengiz1.skyblock.proxy;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Oyuncuyu BungeeCord/Velocity üzerinden başka bir backend sunucuya gönderir.
 * Velocity'de eski "bungeecord" eklenti kanalı varsayılan olarak açıktır.
 */
public class ServerConnector {

    public static final String BUNGEE_CHANNEL = "BungeeCord";

    private final Plugin plugin;

    public ServerConnector(Plugin plugin) {
        this.plugin = plugin;
    }

    public void connect(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, BUNGEE_CHANNEL, out.toByteArray());
    }
}
