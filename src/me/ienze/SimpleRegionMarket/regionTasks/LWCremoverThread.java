package me.ienze.SimpleRegionMarket.regionTasks;

import org.bukkit.World;

import com.griefcraft.model.Protection;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class LWCremoverThread extends Thread {

	private com.griefcraft.lwc.LWC LWC;
	private World world;
	private ProtectedRegion region;
	
	public LWCremoverThread(World wworld, ProtectedRegion region, com.griefcraft.lwc.LWC LWC) {
		super("LWCremoverThread");
		this.world = wworld;
		this.region = region;
		this.LWC = LWC;
	}
	
	public void run() {
		for(int x=region.getMinimumPoint().getBlockX(); x<region.getMaximumPoint().getBlockX(); x++) {
			for(int y=region.getMinimumPoint().getBlockY(); y<region.getMaximumPoint().getBlockY(); y++) {
				for(int z=region.getMinimumPoint().getBlockZ(); z<region.getMaximumPoint().getBlockZ(); z++) {
					remove(world, x, y, z);
				}
			}
		}
	}
	
	public void remove(World world, int x, int y, int z) {
		Protection protection = LWC.findProtection(world, x, y, z);
		if(protection != null) {
			protection.remove();
		}
	}
}
