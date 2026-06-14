package net.cengiz1.skyblock;

import net.cengiz1.skyblock.command.CommandRegistrar;
import net.cengiz1.skyblock.config.ConfigMigrator;
import net.cengiz1.skyblock.config.MessageManager;
import net.cengiz1.skyblock.config.SettingsManager;
import net.cengiz1.skyblock.economy.EconomyHook;
import net.cengiz1.skyblock.economy.NoEconomyHook;
import net.cengiz1.skyblock.economy.VaultEconomyHook;
import net.cengiz1.skyblock.invite.InviteManager;
import net.cengiz1.skyblock.island.IslandManager;
import net.cengiz1.skyblock.island.IslandTimeTask;
import net.cengiz1.skyblock.island.RoleManager;
import net.cengiz1.skyblock.island.VisitService;
import net.cengiz1.skyblock.level.BlockValueManager;
import net.cengiz1.skyblock.level.LevelManager;
import net.cengiz1.skyblock.listener.ListenerRegistrar;
import net.cengiz1.skyblock.menu.MenuManager;
import net.cengiz1.skyblock.proxy.ProxyManager;
import net.cengiz1.skyblock.storage.SqlStorage;
import net.cengiz1.skyblock.storage.Storage;
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
    private VisitService visitService;
    private EconomyHook economy;
    private ProxyManager proxyManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigMigrator.sync(this, "config.yml");
        ConfigMigrator.sync(this, "messages.yml");
        ConfigMigrator.sync(this, "roles.yml");
        ConfigMigrator.sync(this, "levels.yml");
        ConfigMigrator.sync(this, "block-values.yml");
        ConfigMigrator.sync(this, "upgrades.yml");
        reloadConfig();

        this.settings = new SettingsManager(this);
        this.messages = new MessageManager(this);

        try {
            this.storage = new SqlStorage(this, this.settings);
            this.storage.init();
        } catch (Exception error) {
            getLogger().severe("Could not connect to the database: " + error.getMessage());
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
        this.visitService = new VisitService(this);

        this.economy = setupEconomy();
        this.upgradeManager = new UpgradeManager(this);
        this.islandManager.setUpgradeManager(this.upgradeManager);

        this.menuManager = new MenuManager(this);

        this.proxyManager = new ProxyManager(this);
        this.proxyManager.start();

        ListenerRegistrar.registerAll(this);

        CommandRegistrar.register(this);

        new IslandTimeTask(this, this.islandManager, this.settings)
                .runTaskTimer(this, 40L, 40L);

        getLogger().info("Skyblock enabled.");
    }

    @Override
    public void onDisable() {
        if (this.proxyManager != null)
            this.proxyManager.stop();
        if (this.islandManager != null)
            this.islandManager.shutdown();
        if (this.storage != null)
            this.storage.close();
    }

    private EconomyHook setupEconomy() {
        if (!this.settings.isEconomyEnabled()) {
            getLogger().info("Economy disabled in config; money requirements will be skipped.");
            return new NoEconomyHook();
        }
        try {
            EconomyHook hook = VaultEconomyHook.setup();
            if (hook != null) {
                getLogger().info("Hooked into Vault economy.");
                return hook;
            }
        } catch (Throwable error) {
            getLogger().warning("Could not hook into Vault: " + error.getMessage());
        }
        getLogger().info("Vault not found; upgrades will only require island level.");
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

    public VisitService getVisitService() {
        return visitService;
    }

    public EconomyHook getEconomy() {
        return economy;
    }

    public ProxyManager getProxyManager() {
        return proxyManager;
    }
}
