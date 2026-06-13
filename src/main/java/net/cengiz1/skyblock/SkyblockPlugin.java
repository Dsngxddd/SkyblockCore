package net.cengiz1.skyblock;

import net.cengiz1.skyblock.command.IslandCommand;
import net.cengiz1.skyblock.config.MessageManager;
import net.cengiz1.skyblock.config.SettingsManager;
import net.cengiz1.skyblock.island.IslandManager;
import net.cengiz1.skyblock.listener.IslandFlagListener;
import net.cengiz1.skyblock.menu.MenuListener;
import net.cengiz1.skyblock.menu.MenuManager;
import net.cengiz1.skyblock.storage.SqlStorage;
import net.cengiz1.skyblock.storage.Storage;
import net.cengiz1.skyblock.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyblockPlugin extends JavaPlugin {

    private SettingsManager settings;
    private MessageManager messages;
    private Storage storage;
    private WorldManager worldManager;
    private IslandManager islandManager;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.settings = new SettingsManager(this);
        this.messages = new MessageManager(this);

        try {
            this.storage = new SqlStorage(this, this.settings);
            this.storage.init();
        } catch (Exception error) {
            getLogger().severe("Could not connect to storage: " + error.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.worldManager = new WorldManager(this, this.settings);
        this.worldManager.loadWorld();

        this.islandManager = new IslandManager(this, this.settings, this.storage, this.worldManager);
        this.islandManager.loadAll();

        this.menuManager = new MenuManager(this, this.islandManager);

        getServer().getPluginManager().registerEvents(new MenuListener(this.menuManager), this);
        getServer().getPluginManager().registerEvents(new IslandFlagListener(this.islandManager), this);

        getCommand("island").setExecutor(new IslandCommand(this));

        getLogger().info("Skyblock enabled.");
    }

    @Override
    public void onDisable() {
        if (this.islandManager != null)
            this.islandManager.shutdown();
        if (this.storage != null)
            this.storage.close();
    }

    public SettingsManager getSettings() {
        return settings;
    }

    public MessageManager getMessages() {
        return messages;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }
}
