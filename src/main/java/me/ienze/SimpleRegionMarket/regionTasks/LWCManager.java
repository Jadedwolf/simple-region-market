package me.ienze.SimpleRegionMarket.regionTasks;

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public class LWCManager {

    private LWC LWC;

    public void load() {
        //LWC
        Plugin plugin;
        plugin = Bukkit.getServer().getPluginManager().getPlugin("LWC");
        if (plugin != null && plugin instanceof LWCPlugin) {
            LWC = ((LWCPlugin) plugin).getLWC();
        }
    }

    public void removeRegionProtection(ProtectedRegion region, String world) {

        World wworld = Bukkit.getWorld(world);
        if (wworld == null) {
            return;
        }

        new LWCremoverThread(wworld, region, LWC).start();
    }
}
