package me.ienze.SimpleRegionMarket;

import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import me.ienze.SimpleRegionMarket.handlers.CommandHandler;
import me.ienze.SimpleRegionMarket.handlers.ConfigHandler;
import me.ienze.SimpleRegionMarket.handlers.LangHandler;
import me.ienze.SimpleRegionMarket.handlers.LimitHandler;
import me.ienze.SimpleRegionMarket.handlers.ListenerHandler;
import me.ienze.SimpleRegionMarket.regionTasks.LWCManager;
import me.ienze.SimpleRegionMarket.regions.RegionsSaver;
import me.ienze.SimpleRegionMarket.signs.TemplateMain;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Class SimpleRegionMarket.
 */
public class SimpleRegionMarket extends JavaPlugin {

    private static String pluginDir = null;
    private boolean unloading = false;
    private long lastSave;
    // Public classes:
    public static me.ienze.SimpleRegionMarket.handlers.ConfigHandler configurationHandler = null;
    public static WorldGuardManager wgManager = null;
    public static LWCManager lwcManager = null;
    public static RegionsSaver regionSaver = null;
    public static PermissionsManager permManager = null;
    public static EconomyManager econManager = null;
    public static LimitHandler limitHandler = null;
    public static StatisticManager statisticManager = null;
    public static TokenManager tokenManager = null;
    // Private classes:
    private CommandHandler commandHandler;
    private DynMapMarkerManager dynMapMarkers;

    public static String getPluginDir() {
        return pluginDir;
    }

    @Override
    public void onDisable() {
        unloading = true;
        saveAll(null);
        LangHandler.directOut(Level.INFO, "SimpleRegionMarket disabled.");
    }

    @Override
    public void onLoad() {
        SimpleRegionMarket.pluginDir = getDataFolder() + File.separator;

        configurationHandler = new ConfigHandler(this);

        new LangHandler(this, configurationHandler.getConfig().getString("Language", "en"));

        wgManager = new WorldGuardManager();

        lwcManager = new LWCManager();

        if (!configurationHandler.getString("Rollback_On_Expire").equals("none")) {
            regionSaver = new RegionsSaver(Bukkit.getWorlds());
        }

        permManager = new PermissionsManager();

        econManager = new EconomyManager(this);
    }

    @Override
    public void onEnable() {

        try {
            BukkitMetricsLite metrics = new BukkitMetricsLite(this);
            metrics.start();
        } catch (IOException e) {
            // Failed to submit the stats :-(
        }

        lwcManager.load();

        econManager.setupEconomy();

        tokenManager = new TokenManager(this);
        tokenManager.initTemplates();

        statisticManager = new StatisticManager();

        limitHandler = new LimitHandler(this);

        new ListenerHandler(this, tokenManager);

        commandHandler = new CommandHandler(this, tokenManager);
        getCommand("regionmarket").setExecutor(commandHandler);

        Utils.checkOldFilesToUpdate();

        // Check all signs and output stats
        long ms = System.currentTimeMillis();
        final int[] count = tokenManager.checkRegions();
        ms = System.currentTimeMillis() - ms;
        LangHandler.directOut(Level.INFO, "Loaded " + TokenManager.tokenList.size() + " template(s), " + count[0] + " world(s) and " + count[1]
                + " region(s).");
        LangHandler.directOut(Level.INFO, "The check took " + ms + "ms");

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                tokenManager.checkRegions();
                SimpleRegionMarket.limitHandler.autoClear();
            }
        }, 1200L, 1200L);

        //find permission system
        if (SimpleRegionMarket.permManager.loadPermissionSystem(this)) {
            LangHandler.directOut(Level.INFO, "Permissions loaded (" + SimpleRegionMarket.permManager.permission.getName() + ")");
        } else {
            LangHandler.directOut(Level.WARNING, "Permissions plugin not found.");
        }

        //load dynmap markers
        dynMapMarkers = new DynMapMarkerManager(this);
        Plugin dynmap = getServer().getPluginManager().getPlugin("dynmap");
        if (dynmap != null) {
            if (dynmap.isEnabled()) {
                if (this.getConfig().getBoolean("Enable_dynmap", true)) {
                    activateDynMapMarkers();
                }
            }
        }

        LangHandler.directOut(Level.INFO, "SimpleRegionMarket enabled.");
    }

    /**
     * Save all.
     */
    public void saveAll(String onlyWorld) {
        if (!unloading && lastSave + 200000 < System.currentTimeMillis()) {
            lastSave = System.currentTimeMillis();
            if (wgManager.getWorldGuard() != null) {
                if (onlyWorld == null) {
                    LangHandler.consoleOut("Saving all regions.", Level.FINEST, null);
                    for (final World w : getServer().getWorlds()) {
                        final RegionManager mgr = wgManager.getWorldGuard().getRegionManager(w);

                        try {
                            mgr.save();
                        } catch (final ProtectionDatabaseException e) {
                            LangHandler.directOut(Level.SEVERE, "WorldGuard >> Failed to write regionsfile: " + e.getMessage());
                        }
                    }
                } else {
                    LangHandler.consoleOut("Saving regions in world " + onlyWorld + ".", Level.FINEST, null);
                    final RegionManager mgr = wgManager.getWorldGuard().getRegionManager(Bukkit.getWorld(onlyWorld));
                    try {
                        mgr.save();
                    } catch (final ProtectionDatabaseException e) {
                        LangHandler.directOut(Level.SEVERE, "WorldGuard >> Failed to write regionsfile: " + e.getMessage());
                    }
                }
            } else {
                LangHandler.directOut(Level.SEVERE, "Saving WorldGuard failed, because it is not loaded.");
            }
        }

        SimpleRegionMarket.statisticManager.save();

        for (final TemplateMain token : TokenManager.tokenList) {
            token.save();
        }
    }

    public void activateDynMapMarkers() {
        dynMapMarkers.activate();
        LangHandler.directOut(Level.INFO, "Dynmap markers loaded.");
    }
}
