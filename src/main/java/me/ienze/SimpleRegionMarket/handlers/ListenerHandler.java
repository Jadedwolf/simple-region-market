package me.ienze.SimpleRegionMarket.handlers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.PluginEnableEvent;

import me.ienze.SimpleRegionMarket.SimpleRegionMarket;
import me.ienze.SimpleRegionMarket.TokenManager;
import me.ienze.SimpleRegionMarket.Utils;
import me.ienze.SimpleRegionMarket.signs.TemplateMain;

public class ListenerHandler implements Listener {
	private final SimpleRegionMarket plugin;
	private final TokenManager tokenManager;
	
	private HashMap<String, Long> lastPlayerClick = new HashMap<String, Long>();
	private Integer signClickDifference = 1000;

	/**
	 * Instantiates a new listener handler.
	 * 
	 * @param plugin
	 *            the plugin
	 * @param LangHandler
	 *            the lang handler
	 */
	public ListenerHandler(SimpleRegionMarket plugin, TokenManager tokenManager) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		this.plugin = plugin;
		this.tokenManager = tokenManager;
		
		signClickDifference=SimpleRegionMarket.configurationHandler.getConfig().getInt("Clicking_Limit", 1000);
	}

	/**
	 * On block break.
	 * 
	 * @param event
	 *            the BlockBreakEvent
	 */
	@EventHandler
	public void onBlockBreak(final BlockBreakEvent event) {
		final Material type = event.getBlock().getType();
		if (type == Material.SIGN_POST || type == Material.WALL_SIGN) {
			final Location blockLocation = event.getBlock().getLocation();
			final String world = blockLocation.getWorld().getName();
			for (final TemplateMain token : TokenManager.tokenList) {
				if (token.entries.containsKey(world)) {
					for (final String region : token.entries.get(world).keySet()) {
						final ArrayList<Location> signLocations = Utils.getSignLocations(token, world, region);
						if (!signLocations.isEmpty()) {
							for (final Location signLoc : signLocations) {
								if (signLoc.equals(blockLocation)) {
									if (!tokenManager.playerSignBreak(event.getPlayer(), token, world, region, blockLocation)) {
										event.setCancelled(true);
										tokenManager.updateSigns(token, world, region);
									} else {
										plugin.saveAll(null);
									}
									return;
								}
							}
						}
					}
				}
			}
		}
	}


	/**
	 * On player interact.
	 * 
	 * @param event
	 *            the event
	 */
	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent event) {
		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			final Material type = event.getClickedBlock().getType();
			if (type == Material.SIGN_POST || type == Material.WALL_SIGN) {
				final Location blockLocation = event.getClickedBlock().getLocation();
				//final String world = blockLocation.getWorld().getName();
				final Player player = event.getPlayer();
				for (final TemplateMain token : TokenManager.tokenList) {
					for (final String world : token.entries.keySet()) {
						for (final String region : token.entries.get(world).keySet()) {
							final ArrayList<Location> signLocations = Utils.getSignLocations(token, world, region);
							if (!signLocations.isEmpty()) {
								for (final Location signLoc : signLocations) {
									if (signLoc.equals(blockLocation)) {
										//prevent for too much clicking
										for(String key : lastPlayerClick.keySet()) {
											if (lastPlayerClick.get(key)+signClickDifference < System.currentTimeMillis()) {
												lastPlayerClick.remove(key);
											}
										}
										if(!lastPlayerClick.containsKey(player.getName())) {
											lastPlayerClick.put(player.getName(), (long) 0);
										}
										if(lastPlayerClick.get(player.getName())+signClickDifference < System.currentTimeMillis()) {
											lastPlayerClick.put(player.getName(), new Date().getTime());
											tokenManager.playerClickedSign(player, token, world, region);
											plugin.saveAll(world);
											event.setCancelled(true);
											return;
										} else {
											lastPlayerClick.put(player.getName(), System.currentTimeMillis());
											LangHandler.ErrorOut(player, "PLAYER.ERROR.FAST_CLICKS", null);
											return;
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * On sign change.
	 * 
	 * @param event
	 *            the event
	 */
	
	@EventHandler
	public void onSignChange(final SignChangeEvent event) {
		for (final TemplateMain token : TokenManager.tokenList) {
			if (event.getLine(0).equalsIgnoreCase(Utils.getOptionString(token, "input.id"))) {
				final Location signLocation = event.getBlock().getLocation();

				final String lines[] = new String[4];
				for (int i = 0; i < 4; i++) {
					lines[i] = event.getLine(i);
				}

				if (!tokenManager.playerCreatedSign(event.getPlayer(), token, signLocation, lines)) {
					event.getBlock().breakNaturally();
					event.setCancelled(true);
					return;
				}

				for (int i = 0; i < 4; i++) {
					event.setLine(i, ((Sign) event.getBlock().getState()).getLine(i));
				}

				final ArrayList<String> list = new ArrayList<String>();
				final String InputId = Utils.getOptionString(token, "input.id");
				list.add(InputId.toLowerCase().substring(1, InputId.length()-1));
				LangHandler.NormalOut(event.getPlayer(), "PLAYER.REGION.ADDED_SIGN", list);
				plugin.saveAll(null);
				break;
			}
		}
	}
	
	@EventHandler
	public void onPluginEnable(final PluginEnableEvent event) {
		if(event.getPlugin().getName() == "dynmap") {
			if(SimpleRegionMarket.configurationHandler.getConfig().getBoolean("Enable_dynmap", true)) {
				plugin.activateDynMapMarkers();
			}
		}
	}
}
