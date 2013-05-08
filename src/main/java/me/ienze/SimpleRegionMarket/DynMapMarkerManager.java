package me.ienze.SimpleRegionMarket;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.ienze.SimpleRegionMarket.signs.TemplateMain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

/**
 * @author ienze
 *
 */
public class DynMapMarkerManager {

    Plugin dynmap;
    Plugin plugin;
    DynmapAPI api;
    MarkerAPI markerapi;
    String labelfmt;
    MarkerSet ms;
    MarkerIcon markerIcon;
    MarkerIcon markerIconTaken;

    public DynMapMarkerManager(Plugin plugin) {
        this.plugin = plugin;
    }

    void updateMarkerSet() {

        Map<String, Marker> newmap = new HashMap<String, Marker>(); /* Build new map */
        for (TemplateMain token : TokenManager.tokenList) {
            if (!SimpleRegionMarket.configurationHandler.getBoolean("Show_" + token.id)) {
                continue;
            }
            for (final String wname : token.entries.keySet()) {
                for (final String region : token.entries.get(wname).keySet()) {

                    if (region == null) {
                        continue;
                    }

                    ProtectedRegion pr = SimpleRegionMarket.wgManager.getProtectedRegion(Bukkit.getWorld(wname), region);
                    if (pr == null) {
                        continue;
                    }

                    String id = token.id + "/" + wname + "/" + region;

                    //type
                    String label = labelfmt.replace("%type%", token.id);
                    //price
                    Double price = Utils.getEntryDouble(token, wname, region, "price");
                    if (price == 0) {
                        label = label.replace("%price%", "FREE");
                    } else {
                        label = label.replace("%price%", String.valueOf(price));
                    }
                    //time
                    Long renttime = Utils.getEntryLong(token, wname, region, "renttime");
                    Long timeleft = Utils.getEntryLong(token, wname, region, "expiredate") - System.currentTimeMillis();

                    if (renttime != 0) {
                        label = label.replace("%time%", "/" + Utils.getSignTime(renttime));
                    } else if (timeleft > 0) {
                        label = label.replace("%time%", "/" + Utils.getSignTime(timeleft));
                    } else {
                        label = label.replace("%time%", "");
                    }
                    //region
                    label = label.replace("%region%", region);
                    //taken (change icon)
                    MarkerIcon mi;
                    if (Utils.getEntryBoolean(token, wname, region, "taken")) {
                        mi = markerIconTaken;
                    } else {
                        mi = markerIcon;
                    }

                    //sign location
                    int x, y, z;
                    String markerworld;

                    if (SimpleRegionMarket.configurationHandler.getBoolean("Dynmap_Show_On_Region_Loc")) {
                        x = (int) (Math.round((pr.getMaximumPoint().getX() - pr.getMinimumPoint().getX()) / 2) + pr.getMinimumPoint().getX());
                        y = (int) (Math.round((pr.getMaximumPoint().getY() - pr.getMinimumPoint().getY()) / 2) + pr.getMinimumPoint().getY());
                        z = (int) (Math.round((pr.getMaximumPoint().getZ() - pr.getMinimumPoint().getZ()) / 2) + pr.getMinimumPoint().getZ());
                        markerworld = wname;

                    } else {
                        if (Utils.getSignLocations(token, wname, region).isEmpty()) {
                            continue;
                        }

                        Location loc = Utils.getSignLocations(token, wname, region).iterator().next();
                        x = loc.getBlockX();
                        y = loc.getBlockY();
                        z = loc.getBlockZ();
                        markerworld = loc.getWorld().getName();
                    }

                    /* See if we already have marker */
                    Marker m = ms.findMarker(id);
                    if (m == null) { /* Not found?  Need new one */
                        ms.createMarker(id, label, markerworld, x, z, z, mi, false);
                    } else {  /* Else, update position if needed */
                        m.setLocation(wname, x, y, z);
                        m.setLabel(label);
                        m.setMarkerIcon(mi);
                    }
                    newmap.put(id, m);    /* Add to new map */
                }
            }
        }
    }
    long updperiod;
    boolean stop;
    Logger log = Logger.getLogger("Minecraft");
    String LOG_PREFIX = "[SRM] ";

    public void info(String msg) {
        log.log(Level.INFO, LOG_PREFIX + msg);
    }

    public void severe(String msg) {
        log.log(Level.SEVERE, LOG_PREFIX + msg);
    }

    private class MarkerUpdate implements Runnable {

        public void run() {
            if (!stop) {
                updateMarkers();
            }
        }
    }

    /* Update mob population and position */
    private void updateMarkers() {
        updateMarkerSet();
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new MarkerUpdate(), updperiod);
    }

    public void activate() {

        dynmap = plugin.getServer().getPluginManager().getPlugin("dynmap");
        if (dynmap == null) {
            severe("Cannot find dynmap!");
            return;
        }
        api = (DynmapAPI) dynmap;

        /* Now, get markers API */
        markerapi = api.getMarkerAPI();

        if (markerapi == null) {
            severe("Error loading Dynmap marker API!");
            return;
        }

        /* Now, add marker set */
        labelfmt = "%type%: %region% - %price%%time%";
        //layer = new Layer("SimpleRegionMarket", "house");

        /* Set up update job - based on period */
        updperiod = (long) (60.0 * 20.0);
        stop = false;

        markerIcon = markerapi.getMarkerIcon(SimpleRegionMarket.configurationHandler.getConfig().getString("Dynmap_Icon_Free", "sign"));
        markerIconTaken = markerapi.getMarkerIcon(SimpleRegionMarket.configurationHandler.getConfig().getString("Dynmap_Icon_Taken", "basket"));

        ms = markerapi.getMarkerSet("simpleregionmanager");

        if (ms == null) {
            ms = markerapi.createMarkerSet("simpleregionmanager",
                    SimpleRegionMarket.configurationHandler.getString("Markerset_Name"), null, false);
        }

        //first update
        updateMarkerSet();

        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new MarkerUpdate(), 60 * 20);
    }
}
