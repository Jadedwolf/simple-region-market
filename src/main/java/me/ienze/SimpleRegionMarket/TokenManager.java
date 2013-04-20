package me.ienze.SimpleRegionMarket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import me.ienze.SimpleRegionMarket.handlers.LangHandler;
import me.ienze.SimpleRegionMarket.signs.TemplateBid;
import me.ienze.SimpleRegionMarket.signs.TemplateHotel;
import me.ienze.SimpleRegionMarket.signs.TemplateLet;
import me.ienze.SimpleRegionMarket.signs.TemplateMain;
import me.ienze.SimpleRegionMarket.signs.TemplateSell;

public class TokenManager {
	public final static String CONFIG_NAME = "templates.yml";
	public final static File CONFIG_FILE = new File(SimpleRegionMarket.getPluginDir() + CONFIG_NAME);

	public static ArrayList<TemplateMain> tokenList = new ArrayList<TemplateMain>();
	
	private String ListTemplate;

	private final SimpleRegionMarket plugin;

	public TokenManager(SimpleRegionMarket plugin) {
		this.plugin = plugin;
	}

	/**
	 * Update signs from tokens
	 * 
	 * @param agent
	 *            the agent
	 * @param event
	 *            the event
	 */
	public void updateSigns(TemplateMain token, String world, String region) {
		if (token != null && world != null && region != null) {

			final World worldWorld = Bukkit.getWorld(world);
			if (worldWorld == null) {
				return;
			}

			final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(worldWorld, region);
			if (protectedRegion == null) {
				return;
			}

			// Parse sign lines
			final String[] lines = new String[Utils.SIGN_LINES];
			for (int i = 0; i < Utils.SIGN_LINES; i++) {
				String line;
				if (Utils.getEntryBoolean(token, world, region, "taken")) {
					String tLine = Utils.getOptionString(token, "taken." + (i + 1));
					if(tLine.contains("&")) {
						line = ChatColor.translateAlternateColorCodes('&', tLine);
					} else {
						String t = SimpleRegionMarket.configurationHandler.getString("Taken_Color");
						String takenColor = ChatColor.translateAlternateColorCodes('&', t);
						line = takenColor + tLine;
					}
				} else {
					String oLine = Utils.getOptionString(token, "output." + (i + 1));
					if(oLine.contains("&")) {
						line = ChatColor.translateAlternateColorCodes('&', oLine);
					} else {
						String t = SimpleRegionMarket.configurationHandler.getString("Output_Color");
						String outputColor = ChatColor.translateAlternateColorCodes('&', t);
						line = outputColor + oLine;
					}
				}
				lines[i] = Utils.replaceTokens(line, token.getReplacementMap(world, region));
			}

			// Set sign lines for all signs
			if (Utils.getEntry(token, world, region, "signs") != null) {
				for (final Location loc : Utils.getSignLocations(token, world, region)) {
					if (loc.getBlock().getType() != Material.SIGN_POST && loc.getBlock().getType() != Material.WALL_SIGN) {
						loc.getBlock().setType(Material.SIGN_POST);
						LangHandler.directOut(Level.INFO, "Can't find sign, creating new.");
					} else {
						Block bloque = (Block) loc.getBlock();
						if (bloque.getType() == Material.WALL_SIGN || bloque.getType() == Material.SIGN_POST) {
							//Sometimes crash cause sign is not in the place and cant put it
							try{
								Sign sign = (Sign) bloque.getState();
								for (int i = 0; i < Utils.SIGN_LINES; i++) {
									sign.setLine(i, lines[i]);
								}
								sign.update();
							}
							catch(Exception ex){
								LangHandler.consoleOut("SRM Sign was Crashed, deleting sign for security -> "+bloque.toString(), Level.WARNING, null);
								//Deleting crashed Sign
								final ArrayList<Location> signLocations = Utils.getSignLocations(token, world, region);
								Utils.removeEntry(token, world, region, "signs");
								Utils.removeRegion(token, world, region);
								Location signLocation = new Location(worldWorld,bloque.getX(),bloque.getY(),bloque.getZ());
								signLocations.remove(signLocation);
							}
						}
					}
				}
			}
		}
	}

	public List<String> getRegionList(Integer page, Integer limit, TemplateMain token, String world, HashMap<String, String> data, Location playerLocation) {
		ListTemplate = SimpleRegionMarket.configurationHandler.getString("Regions_List_Template");
		ArrayList<String> out = new ArrayList<String>();
		int count = 0;
		
		final World worldWorld = Bukkit.getWorld(world);
		if (worldWorld == null) {
			LangHandler.directOut(Level.WARNING, "CHECK.WARN.NO_WORLD"+world);
		} else {
			for (final String region : token.entries.get(world).keySet()) {
				if(!Utils.getEntryBoolean(token, world, region, "hidden")) {
					final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(worldWorld, region);
					if (protectedRegion == null) {
						LangHandler.consoleOut("CHECK.WARN.NO_REGION", Level.WARNING, null);
					} else {
							
						if(data.containsKey("minprice")) {
							if(Utils.getEntryDouble(token, world, region, "price") < Double.parseDouble(data.get("minprice")))
								continue;
						}
						
						if(data.containsKey("maxprice")) {
							if(Utils.getEntryDouble(token, world, region, "price") > Double.parseDouble(data.get("maxprice")))
								continue;
						}
						
						if(data.containsKey("minrenttime")) {
							if(Utils.getEntryLong(token, world, region, "renttime") < Utils.parseSignTime(data.get("minrenttime")))
								continue;
						}
						
						if(data.containsKey("maxrenttime")) {
							if(Utils.getEntryLong(token, world, region, "renttime") > Utils.parseSignTime(data.get("maxrenttime")))
								continue;
						}
						
						if(data.containsKey("radius")) {
							if(playerLocation != null) {
								ProtectedRegion pr = Utils.getProtectedRegion(region, null, world);
								if(pr != null) {
									Location regionLoc = new Location(Bukkit.getWorld(world), pr.getMinimumPoint().getBlockX(), pr.getMinimumPoint().getBlockY(), pr.getMinimumPoint().getBlockZ());
									if(regionLoc.distance(playerLocation) > Double.parseDouble(data.get("radius"))) {
										continue;
									}
								}
							}
						}
						
						if(data.containsKey("minradius")) {
							if(playerLocation != null) {
								ProtectedRegion pr = Utils.getProtectedRegion(region, null, world);
								if(pr != null) {
									Location regionLoc = new Location(Bukkit.getWorld(world), pr.getMinimumPoint().getBlockX(), pr.getMinimumPoint().getBlockY(), pr.getMinimumPoint().getBlockZ());
									if(regionLoc.distance(playerLocation) < Double.parseDouble(data.get("minradius"))) {
										continue;
									}
								}
							}
						}
						
						count++;
						if(count > (page*limit)-limit && count < (page*limit)) {
							out.add(getRegionInfoLine(world, region,token));
						}
						
						if(count > (page*limit)) {
							break;
						}
					}
				}
			}
		}
		List<String> newList = Arrays.asList(out.toArray(new String[out.size()]));
		return newList;
	}
	
	private String getRegionInfoLine(String world, String region, TemplateMain token) {
		String label = ListTemplate;
		
		Long rentTime = Utils.getEntryLong(token, world, region, "renttime");
		Long expireTime = Utils.getEntryLong(token, world, region, "expiredate");
		
		String time = "";
		if(rentTime != null && rentTime > 0) {
			time = "/" + Utils.getSignTime(rentTime);
		}
		if(expireTime != null && expireTime > 0) {
			time = "/" + Utils.getSignTime(expireTime - System.currentTimeMillis());
		}
		
		label = label.replace("&region&", region);
		label = label.replace("&tokenType&", token.id);
		label = label.replace("&price&", Utils.getEntryString(token, world, region, "price"));
		label = label.replace("&time&", time);

		return label;
	}
	
	public int[] checkRegions() {
		final int[] count = new int[2];
		final ArrayList<String> worldsHad = new ArrayList<String>();
		final ArrayList<String> regionsHad = new ArrayList<String>();
		for (final TemplateMain token : TokenManager.tokenList) {
			for (final String world : token.entries.keySet()) {
				final World worldWorld = Bukkit.getWorld(world);
				if (worldWorld == null) {
					if (!worldsHad.contains(world)) {
						final ArrayList<String> list = new ArrayList<String>();
						list.add(world);
						LangHandler.directOut(Level.WARNING, "CHECK.WARN.NO_WORLD"+world);
					}
					token.entries.remove(world);
				} else {
					if (!worldsHad.contains(world)) {
						count[0]++;
					}
					String region;
					Iterator<String> regioni = token.entries.get(world).keySet().iterator();
					try {
						while (regioni.hasNext()) {
							region = regioni.next();
							final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(worldWorld, region);
							if (protectedRegion == null) {
								if (!regionsHad.contains(region)) {
									final ArrayList<String> list = new ArrayList<String>();
									list.add(region);
									LangHandler.consoleOut("CHECK.WARN.NO_REGION", Level.WARNING, list);
								}
								token.entries.get(world).remove(region);
							} else {
								if (!regionsHad.contains(region)) {
									count[1]++;
								}
								token.schedule(world, region);
								updateSigns(token, world, region);
								
								//remove taken region (sell, bid)
								if(SimpleRegionMarket.configurationHandler.getString("Auto_Removing_Regions").contains(Utils.getOptionString(token, "type"))) {
									if(Utils.getEntryBoolean(token, world, region, "taken")) {
										Utils.setEntry(token, world, region, "hidden", true);
									} else {
										Utils.setEntry(token, world, region, "hidden", false);
									}
								}
							}
							if (!regionsHad.contains(region)) {
								regionsHad.add(region);
							}
						}
					} catch (ConcurrentModificationException e) {
						
					}
				}
				if (!worldsHad.contains(world)) {
					worldsHad.add(world);
				}
			}
		}
		return count;
	}

	public void initTemplates() {
	
		//updating files templates to new version
		FileConfiguration templatesFile = new YamlConfiguration();
		try {
			templatesFile.load(SimpleRegionMarket.getPluginDir() + CONFIG_NAME);
		} catch (final FileNotFoundException e) {
			LangHandler.directOut(Level.INFO, "TEMPLATES.NORM.NO_TEMPLATES");
			plugin.saveResource(CONFIG_NAME, false);
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InvalidConfigurationException e) {
			e.printStackTrace();
		}
		String templatesVersion = templatesFile.getString("templates_version");
		//resources
		try {
			templatesFile.load(plugin.getResource(CONFIG_NAME));
		} catch (final IOException e) {
			e.printStackTrace();
			return;
		} catch (final InvalidConfigurationException e) {
			e.printStackTrace();
			return;
		}
		String templatesResourceVersion = templatesFile.getString("templates_version");
		
		if(templatesVersion == null || Utils.compareVersions(templatesResourceVersion, templatesVersion) == 1) {
			plugin.saveResource(CONFIG_NAME, true);
		}
		
		if (CONFIG_FILE.exists()) {
			final YamlConfiguration configHandle = YamlConfiguration.loadConfiguration(CONFIG_FILE);
			for (final String key : configHandle.getKeys(false)) {
				if(!key.equalsIgnoreCase("templates_version")) {
					final String type = configHandle.getString(key + ".type");
					if (type.equalsIgnoreCase("sell")) {
						tokenList.add(new TemplateSell(plugin, this, key));
					} else if (type.equalsIgnoreCase("let")) {
						tokenList.add(new TemplateLet(plugin, this, key));
					} else if (type.equalsIgnoreCase("hotel") || type.equalsIgnoreCase("rent")) {
						tokenList.add(new TemplateHotel(plugin, this, key));
					} else if (type.equalsIgnoreCase("bid")) {
						tokenList.add(new TemplateBid(plugin, this, key));
					} else {
						final ArrayList<String> list = new ArrayList<String>();
						list.add(type);
						LangHandler.consoleOut("TEMPLATES.WARN.TYPE_NOT_KOWN", Level.WARNING, list);
					}
				}
			}
		} else {
			LangHandler.directOut(Level.WARNING, "TEMPLATES.ERROR.NO_STANDARD_TEMPLATES");
		}
	}

	public boolean playerIsOwner(Player player, TemplateMain token, String world, ProtectedRegion protectedRegion) {
		if (player != null && token != null && world != null && protectedRegion != null) {
			if(protectedRegion.getOwners().size() > 0) {
				if (protectedRegion.isOwner(SimpleRegionMarket.wgManager.wrapPlayer(player))) {
					return true;
				}
			}
			
			if(player.getName().equals(Utils.getEntryString(token, world, protectedRegion.getId(), "owner"))) {
				return true;
			}
			
			if(SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
				return true;
			}
		}
		return false;
	}

	public void playerClickedSign(Player player, TemplateMain token, String world, String region) {
		if (Utils.getEntryBoolean(token, world, region, "taken")) {
			if (playerIsOwner(player, token, world, SimpleRegionMarket.wgManager.getProtectedRegion(Bukkit.getWorld(world), region))) {
				token.ownerClicksTakenSign(world, region);
			} else {
				token.otherClicksTakenSign(player, world, region);
			}
		} else {
			if (playerIsOwner(player, token, world, SimpleRegionMarket.wgManager.getProtectedRegion(Bukkit.getWorld(world), region))) {
				token.ownerClicksSign(player, world, region);
			} else {
				// Limits
				String[] groups = SimpleRegionMarket.permManager.getPlayerGroups(player);
				String m = "PLAYER.LIMITS.GLOBAL";
				if (SimpleRegionMarket.limitHandler.checkPerms(player, groups, token, m)) {
					token.otherClicksSign(player, world, region);
				} else {
					LangHandler.ErrorOut(player, m, null);
				}
			}
		}
	}

	public boolean playerCreatedSign(Player player, TemplateMain token, Location signLocation, String[] lines) {
		String world = null;
		final HashMap<String, String> input = Utils.getSignInput(token, lines);

		if(input.get("region").isEmpty()) {
			//check if sign is in some region
			RegionManager worldRegionManager = SimpleRegionMarket.wgManager.getWorldGuard().getRegionManager(signLocation.getWorld());
			if(worldRegionManager.getApplicableRegions(signLocation).size() > 0) {
				ApplicableRegionSet ApplicableRegions = worldRegionManager.getApplicableRegions(signLocation);
				ProtectedRegion r = ApplicableRegions.iterator().next();
				if(r != null) {
					input.put("region", r.getId());
				}
			}
		}
		
		if(input.get("world") == null) {
			if(SimpleRegionMarket.configurationHandler.getBoolean("Use_Other_World_Regions")) {
				for(World w : Bukkit.getWorlds()) {
					final ProtectedRegion testRegion = Utils.getProtectedRegion(input.get("region").toString(), signLocation, w.getName());
					if(testRegion != null) {
						world = w.getName();
						break;
					}
				}
				if(world == null) {
					world = signLocation.getWorld().getName();
				}
			} else {
				world = signLocation.getWorld().getName();
			}
		} else {
			world = input.get("world");
		}
		
		final ProtectedRegion protectedRegion = Utils.getProtectedRegion(input.get("region").toString(), signLocation, world);

		if (protectedRegion == null) {
			LangHandler.ErrorOut(player, "PLAYER.ERROR.NO_REGION", null);
			return false;
		}

		final String region = protectedRegion.getId();

		//remove Auto_Removing_Regions
		for (final TemplateMain otherToken : TokenManager.tokenList) {
			
			Boolean hidden = Utils.getEntryBoolean(otherToken, world, region, "hidden");
			if(hidden == null) {
				hidden = false;
			}
			if(hidden) {
				if(SimpleRegionMarket.configurationHandler.getString("Auto_Removing_Regions").contains(otherToken.id)) {
					for(Location sign : Utils.getSignLocations(otherToken, world, region)) {
						sign.getBlock().breakNaturally();
					}
					otherToken.entries.get(world).remove(region);
				}
			}
		}
		
		for (final TemplateMain otherToken : TokenManager.tokenList) {
			if (!otherToken.equals(token) && Utils.getEntry(otherToken, world, region, "taken") != null) {
				if (!Utils.getSignLocations(otherToken, world, region).isEmpty() || Utils.getEntryBoolean(otherToken, world, region, "taken")) {
					LangHandler.ErrorOut(player, "PLAYER.ERROR.OTHER_TOKEN", null);
					return false;
				}
			}
		}
		
		// Permissions
		if (!SimpleRegionMarket.permManager.canPlayerCreateSign(player, lines[0].substring(1, lines[0].length()-1).toLowerCase())) {
			LangHandler.ErrorOut(player, "PLAYER.NO_PERMISSIONS", null);
			return false;
		}

		if(!playerIsOwner(player, token, region, protectedRegion)) {
			LangHandler.ErrorOut(player, "PLAYER.ERROR.NOT_OWNER", null);
			return false;
		}
		
		return token.signCreated(player, world, protectedRegion, signLocation, input, lines);
	}

	public boolean playerSignBreak(Player player, TemplateMain token, String world, String region, Location signLocation) {
		final World worldWorld = Bukkit.getWorld(world);
		final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(worldWorld, region);

		// Permissions
		if (!SimpleRegionMarket.permManager.hadAdminPermissions(player) && !playerIsOwner(player, token, world, protectedRegion)) {
			LangHandler.ErrorOut(player, "PLAYER.ERROR.NOT_OWNER", null);
			return false;
		}

		final ArrayList<Location> signLocations = Utils.getSignLocations(token, world, region);
		if (signLocations.size() == 1) {
			if (Utils.getEntryBoolean(token, world, region, "taken")) {
				if (token.canLiveWithoutSigns) {
					Utils.removeEntry(token, world, region, "signs");
				} else {
					LangHandler.ErrorOut(player, "PLAYER.ERROR.NEED_ONE_SIGN", null);
					return false;
				}
			} else {
				Utils.removeRegion(token, world, region);
				LangHandler.Out(player, ChatColor.WHITE, "PLAYER.REGION.DELETED");
				return true;
			}
		} else {
			signLocations.remove(signLocation);
		}
		LangHandler.Out(player, ChatColor.WHITE, "PLAYER.REGION.DELETED_SIGN");
		return true;
	}
}
