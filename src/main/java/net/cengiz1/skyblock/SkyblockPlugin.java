package net.cengiz1.skyblock;

import net.cengiz1.skyblock.command.CommandRegistrar;
import net.cengiz1.skyblock.config.MessageManager;
import net.cengiz1.skyblock.config.SettingsManager;
import net.cengiz1.skyblock.economy.EconomyHook;
import net.cengiz1.skyblock.economy.NoEconomyHook;
import net.cengiz1.skyblock.economy.VaultEconomyHook;
import net.cengiz1.skyblock.invite.InviteManager;
import net.cengiz1.skyblock.island.IslandManager;
import net.cengiz1.skyblock.island.IslandTimeTask;
import net.cengiz1.skyblock.island.RoleManager;
import net.cengiz1.skyblock.level.BlockTrackListener;
import net.cengiz1.skyblock.level.BlockValueManager;
import net.cengiz1.skyblock.level.LevelManager;
import net.cengiz1.skyblock.listener.IslandFlagListener;
import net.cengiz1.skyblock.menu.MenuListener;
import net.cengiz1.skyblock.menu.MenuManager;
import net.cengiz1.skyblock.storage.SqlStorage;
import net.cengiz1.skyblock.storage.Storage;
import net.cengiz1.skyblock.upgrade.UpgradeEffectListener;
import net.cengiz1.skyblock.upgrade.UpgradeManager;
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
    private BlockValueManager blockValueManager;
    private LevelManager levelManager;
    private UpgradeManager upgradeManager;
    private RoleManager roleManager;
    private InviteManager inviteManager;
    private EconomyHook economy;

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
            getLogger().severe("Veritabanına bağlanılamadı: " + error.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.worldManager = new WorldManager(this, this.settings);
        this.worldManager.loadWorld();

        this.islandManager = new IslandManager(this, this.settings, this.storage, this.worldManager);
        this.islandManager.loadAll();

        this.blockValueManager = new BlockValueManager(this);
        this.levelManager = new LevelManager(this);
        this.roleManager = new RoleManager(this);
        this.inviteManager = new InviteManager(this.settings.getInviteExpireSeconds());

        this.economy = setupEconomy();
        this.upgradeManager = new UpgradeManager(this);
        this.islandManager.setUpgradeManager(this.upgradeManager);

        this.menuManager = new MenuManager(this);

        getServer().getPluginManager().registerEvents(new MenuListener(this.menuManager), this);
        getServer().getPluginManager().registerEvents(new IslandFlagListener(this.islandManager), this);
        getServer().getPluginManager().registerEvents(
                new BlockTrackListener(this, this.islandManager, this.blockValueManager, this.levelManager), this);
        getServer().getPluginManager().registerEvents(
                new UpgradeEffectListener(this.islandManager, this.upgradeManager), this);

        CommandRegistrar.register(this);

        // Ada zamanı (gece/gündüz) uygulayıcısı — her 2 saniyede bir.
        new IslandTimeTask(this, this.islandManager, this.settings)
                .runTaskTimer(this, 40L, 40L);

        getLogger().info("Skyblock etkinleştirildi.");
    }

    @Override
    public void onDisable() {
        if (this.islandManager != null)
            this.islandManager.shutdown();
        if (this.storage != null)
            this.storage.close();
    }

    private EconomyHook setupEconomy() {
        if (!this.settings.isEconomyEnabled()) {
            getLogger().info("Ekonomi config'de kapalı; para şartı uygulanmayacak.");
            return new NoEconomyHook();
        }
        try {
            EconomyHook hook = VaultEconomyHook.setup();
            if (hook != null) {
                getLogger().info("Vault ekonomisine bağlanıldı.");
                return hook;
            }
        } catch (Throwable error) {
            getLogger().warning("Vault bağlanamadı: " + error.getMessage());
        }
        getLogger().info("Vault bulunamadı; yükseltmeler yalnızca seviye şartıyla çalışacak.");
        return new NoEconomyHook();
    }

    public void reloadAll() {
        reloadConfig();
        this.settings.reload();
        this.messages.reload();
        this.blockValueManager.reload();
        this.levelManager.reload();
        this.upgradeManager.reload();
        this.roleManager.reload();
        this.menuManager.reload();
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

    public BlockValueManager getBlockValueManager() {
        return blockValueManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public RoleManager getRoleManager() {
        return roleManager;
    }

    public InviteManager getInviteManager() {
        return inviteManager;
    }

    public EconomyHook getEconomy() {
        return economy;
    }
}
