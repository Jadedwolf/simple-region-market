package me.ienze.SimpleRegionMarket.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.ienze.SimpleRegionMarket.SimpleRegionMarket;
import me.ienze.SimpleRegionMarket.TokenManager;
import me.ienze.SimpleRegionMarket.Utils;
import me.ienze.SimpleRegionMarket.signs.TemplateMain;

public class CommandHandler implements CommandExecutor {
	private final SimpleRegionMarket plugin;
	private final TokenManager tokenManager;
	
	private String confirmCommand;
	private Long confirmTime;

	/**
	 * Instantiates a new command handler.
	 * 
	 * @param plugin
	 *            the plugin
	 * @param LangHandler
	 *            the lang handler
	 */
	public CommandHandler(SimpleRegionMarket plugin, TokenManager tokenManager) {
		this.plugin = plugin;
		this.tokenManager = tokenManager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		Player player = null;
		Boolean isConsole = false;
		if (sender instanceof Player) {
			player = (Player) sender;
		} else {
			isConsole = true;
		}

		//no any arguments - Menue
		if (args.length < 1) {
			String[] text = new String[] {
				plugin.getDescription().getName() + " Version " + plugin.getDescription().getVersion(),
				Utils.getCopyright(),
				"For more info use /srm help"
			};
			
			LangHandler.Out(sender, ChatColor.GREEN, text);
			
			return true;
		}
		
		if (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("untake")) {
			if (isConsole || SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
				if (args.length < 2) {
					if (isConsole) {
						LangHandler.consoleOut("CMD.UNTAKE.NO_ARG", Level.INFO, null);
					} else {
						LangHandler.ListOut(player, "CMD.UNTAKE.NO_ARG", null);
					}
					return true;
				} else {
					final String region = args[1];
					String world;
					if (args.length > 2) {
						world = args[2];
					} else {
						if (isConsole) {
							LangHandler.consoleOut("COMMON.CONSOLE_NOWORLD", Level.SEVERE, null);
							return true;
						} else {
							world = player.getWorld().getName();
						}
					}
					Boolean found = false;
					for (final TemplateMain token : TokenManager.tokenList) {
						if (Utils.getEntry(token, world, region, "taken") != null) {
							if (Utils.getEntryBoolean(token, world, region, "taken")) {
								token.untakeRegion(world, region);
								found = true;
								break;
							}
						}
					}
					final ArrayList<String> list = new ArrayList<String>();
					list.add(region);
					list.add(world);
					if (found) {
						if (isConsole) {
							LangHandler.consoleOut("CMD.UNTAKE.SUCCESS", Level.INFO, list);
						} else {
							LangHandler.ListOut(player, "CMD.UNTAKE.SUCCESS", list);
						}
					} else {
						if (isConsole) {
							LangHandler.consoleOut("COMMON.NO_REGION", Level.WARNING, list);
						} else {
							LangHandler.ErrorOut(player, "COMMON.NO_REGION", list);
						}
					}
				}
			} else {
				LangHandler.ErrorOut(player, "PLAYER.NO_PERMISSIONS", null);
			}
			
		} else if (args[0].equalsIgnoreCase("clearall") || args[0].equalsIgnoreCase("untakeall")) {
			if (isConsole || SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
				confirmCommand = "clear";
				confirmTime = System.currentTimeMillis();
				
				LangHandler.Out(sender, ChatColor.GREEN, "Clear all regions. Do /srm confirm - to confirm operation.");
			} else {
				LangHandler.ErrorOut(player, "PLAYER.NO_PERMISSIONS", null);
			}
			
		} else if (args[0].equalsIgnoreCase("remove")) {
			if (isConsole || SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
				if (args.length < 2) {
					if (isConsole) {
						LangHandler.consoleOut("CMD.REMOVE.NO_ARG", Level.INFO, null);
					} else {
						LangHandler.ListOut(player, "CMD.REMOVE.NO_ARG", null);
					}
					return true;
				} else {
					final String region = args[1];
					String world;
					if (args.length > 2) {
						world = args[2];
					} else {
						if (isConsole) {
							LangHandler.consoleOut("COMMON.CONSOLE_NOWORLD", Level.SEVERE, null);
							return true;
						} else {
							world = player.getWorld().getName();
						}
					}
					Boolean found = false;
					for (final TemplateMain token : TokenManager.tokenList) {
						if (Utils.getEntry(token, world, region, "taken") != null) {
							if (Utils.getEntryBoolean(token, world, region, "taken")) {
								token.untakeRegion(world, region);
								Utils.removeRegion(token, world, region);
								found = true;
								break;
							}
						}
					}
					final ArrayList<String> list = new ArrayList<String>();
					list.add(region);
					list.add(world);
					if (found) {
						if (isConsole) {
							LangHandler.consoleOut("CMD.REMOVE.SUCCESS", Level.INFO, list);
						} else {
							LangHandler.ListOut(player, "CMD.REMOVE.SUCCESS", list);
						}
					} else {
						if (isConsole) {
							LangHandler.consoleOut("COMMON.NO_REGION", Level.WARNING, list);
						} else {
							LangHandler.ErrorOut(player, "COMMON.NO_REGION", list);
						}
					}
				}
			} else {
				LangHandler.ErrorOut(player, "PLAYER.NO_PERMISSIONS", null);
			}
			
		} else if (args[0].equalsIgnoreCase("removeall")) {
			if (isConsole || SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
				confirmCommand = "remove";
				confirmTime = System.currentTimeMillis();
				
				LangHandler.Out(sender, ChatColor.GREEN, "Remove all regions. Do /srm confirm - to confirm operation.");
			} else {
				LangHandler.ErrorOut(player, "PLAYER.NO_PERMISSIONS", null);
			}
			
		} else if (args[0].equalsIgnoreCase("regen") || args[0].equalsIgnoreCase("regenerate")) {
			if (isConsole || SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
				if (args.length < 2) {
					if (isConsole) {
						LangHandler.consoleOut("CMD.REMOVE.NO_ARG", Level.INFO, null);
					} else {
						LangHandler.ListOut(player, "CMD.REMOVE.NO_ARG", null);
					}
					return true;
				} else {
					final String region = args[1];
					String world;
					if (args.length > 2) {
						world = args[2];
					} else {
						if (isConsole) {
							LangHandler.consoleOut("COMMON.CONSOLE_NOWORLD", Level.SEVERE, null);
							return true;
						} else {
							world = player.getWorld().getName();
						}
					}
					
					ProtectedRegion pr = Utils.getProtectedRegion(region, null, world);
					
					if (pr == null) {
						final ArrayList<String> list = new ArrayList<String>();
						list.add(region);
						list.add(world);
						if (isConsole) {
							LangHandler.consoleOut("COMMON.NO_REGION", Level.WARNING, list);
						} else {
							LangHandler.ErrorOut(player, "COMMON.NO_REGION", list);
						}
					} else {
						//start new regeneration thread
						//new TerrainRegeneratorThread(Bukkit.getWorld(world), pr).start();
						LangHandler.Out(sender, ChatColor.DARK_GRAY, "Sorry, I working on this command, but it isn't enabled for public use...");
					}
				}
			} else {
				LangHandler.ErrorOut(player, "PLAYER.NO_PERMISSIONS", null);
			}
			
		} else if (args[0].equalsIgnoreCase("confirm")) {
			if (isConsole || SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
				if(confirmCommand != null && confirmTime + 90000 > System.currentTimeMillis()) {
					if(confirmCommand == "clearstatistics") {
						SimpleRegionMarket.statisticManager.clear();
						LangHandler.Out(sender, ChatColor.GREEN, "Statistics cleared.");
					} else if(confirmCommand == "clearlimits") {
						SimpleRegionMarket.limitHandler.reloadLimits();
						LangHandler.Out(sender, ChatColor.GREEN, "Limits cleared.");
					} else if (confirmCommand == "remove" || confirmCommand == "clear"){
						for (final TemplateMain token : TokenManager.tokenList) {
							if(token != null) {
								for (final String world : token.entries.keySet()) {
									for (final String region : token.entries.get(world).keySet()) {
										if(confirmCommand == "remove") {
											Utils.removeRegion(token, world, region);
										} else if(confirmCommand == "clear") {
											if (Utils.getEntryBoolean(token, world, region, "taken")) {
												token.untakeRegion(world, region);
												break;
											}
										}
									}
								}
							}
						}
					LangHandler.Out(sender, ChatColor.GREEN, "All regions removed/cleared.");
					}
				
					confirmCommand = null;
					confirmTime = 0L;
					
				} else {
					if(player != null) {
						LangHandler.ErrorOut(player, "No anything to confirm.", null);
					} else {
						LangHandler.directOut(Level.WARNING, "No anything to confirm.");
					}
				}
			} else {
				LangHandler.ErrorOut(player, "PLAYER.NO_PERMISSIONS", null);
			}
			
		} else if (args[0].equalsIgnoreCase("clearstatistics")) {
			
			if (isConsole || SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
				confirmCommand = "clearstatistics";
				confirmTime = System.currentTimeMillis();
				
				LangHandler.Out(sender, ChatColor.GREEN, "Clear statistics. Do /srm confirm - to confirm operation.");
			} else {
				LangHandler.ErrorOut(player, "PLAYER.NO_PERMISSIONS", null);
			}
			
		} else if (args[0].equalsIgnoreCase("clearlimits")) {
			if (isConsole || SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
				confirmCommand = "clearlimits";
				confirmTime = System.currentTimeMillis();
				
				LangHandler.Out(sender, ChatColor.GREEN, "Clear all limits. Do /srm confirm - to confirm operation.");
			} else {
				LangHandler.ErrorOut(player, "PLAYER.NO_PERMISSIONS", null);
			}
			
		} else if (args[0].equalsIgnoreCase("help")) {
			
			String[] help;
			int page = 1;
			
			if(args.length == 2) {
				page = Integer.parseInt(args[1]);
			}
		
			if(page == 1) {
				help = new String[] {
					"--- Help --- 1/2",
					"/srm - Informations",
					"/srm limits - List of your limits",
					"/srm list <page> <market_type> - List of all regions",
					"/srm addMember <player> <region> - add member to your region",
					"/srm remMember <player> <region> - remove member from your region",
					"/srm statistics [global/userBuy/userSell] - Show statstic",
					"/srm clear [region_name] - Remove members from region",
					"/srm remove [region_name] - Remove region from market",
					"To see next page of help typ /srm help 2"
				};
			} else if (page == 2) {
				help = new String[] {
						"--- Help --- 2/2",
						"/srm gs <sign> - teleport to free region",
						"/srm tp <region> - teleport to your region",
						"/srm reload - Reloads the config-file",
						"/srm clearall - Remove members from all regions",
						"/srm removeall - Remove all region from market",
						"/srm clearstatistics - Clear statstic",
						"/srm clearlimits - Reset limits",
					};
			} else {
				help = new String[] {"Can't find page "+page};
			}
			
			if (player == null) {
				LangHandler.Out(sender, ChatColor.GREEN, help);
			} else {
				LangHandler.Out(player, ChatColor.GREEN, help);
			}
			
		} else if (args[0].equalsIgnoreCase("reload")) {

			if(isConsole || SimpleRegionMarket.permManager.hadAdminPermissions(player)){
				SimpleRegionMarket.statisticManager.save();
				SimpleRegionMarket.configurationHandler.ReloadConfigHandler();
				SimpleRegionMarket.limitHandler = new LimitHandler(plugin);
				if(player != null) {
					LangHandler.NormalOut(player, "CMD.RELOAD.SUCCESS", null);
				} else {
					LangHandler.consoleOut("CMD.RELOAD.SUCCESS", Level.INFO, null);
				}
			}  else {
				LangHandler.ErrorOut(player, "PLAYER.NO_PERMISSIONS", null);
			}
			
		} else if (args[0].equalsIgnoreCase("statistics") || args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("stat")) {
			
			if(SimpleRegionMarket.configurationHandler.getBoolean("Statistics_Enabled")) {
			
				if(isConsole || SimpleRegionMarket.permManager.canPlayerCommandAbout(player)) {
					if(args.length != 3 && args.length != 2) {
						String[] lines = new String[] {
								"What do you want to see? ",
								"  /srm statistics global/userBuy/userSell <player>"
						};
						
						LangHandler.Out(sender, ChatColor.GREEN, lines);
					} else {
						
						String aboutPlayer = player.getName();
						
						if(isConsole || SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
							if(args.length > 3) {
								aboutPlayer = args[2];
							}
						}
						
						ArrayList<String> out = new ArrayList<String>();
						String statisticsType = args[1];
						
						if(statisticsType.equalsIgnoreCase("global")) {
							//global transactions
							out.add("Global transactions:");
							out.add("  All: " + getStatisticsEntry("global.token.all"));
							for (final TemplateMain token : TokenManager.tokenList) {
								out.add("  " + token.id + ": " + getStatisticsEntry("global.token."+token.id));
							}
							
							//global price
							out.add("global moneys used:");
							out.add("  All: " + getStatisticsEntry("global.price.all"));
							for (final TemplateMain token : TokenManager.tokenList) {
								out.add("  " + token.id + ": " + getStatisticsEntry("global.price."+token.id));
							}
							
						} else if(statisticsType.equalsIgnoreCase("userBuy")) {
							
							//user buy transactions
							out.add(aboutPlayer+"s buy transactions:");
							out.add("  All: " + getStatisticsEntry("users."+aboutPlayer+".buyedtokens.all"));
							for (final TemplateMain token : TokenManager.tokenList) {
								out.add("  " + token.id + ": " + getStatisticsEntry("users."+aboutPlayer+".buyedtokens."+token.id));
							}
							
							//user buy price
							out.add(aboutPlayer+"s buy price:");
							out.add("  All: " + getStatisticsEntry("users."+aboutPlayer+".payedprice.all"));
							for (final TemplateMain token : TokenManager.tokenList) {
								out.add("  " + token.id + ": " + getStatisticsEntry("users."+aboutPlayer+".payedprice."+token.id));
							}
							
						} else if(statisticsType.equalsIgnoreCase("userSell")) {
							
							//user sell transactions
							out.add(aboutPlayer+"s sell transactions:");
							out.add("  All: " + getStatisticsEntry("users."+aboutPlayer+".selledtokens.all"));
							for (final TemplateMain token : TokenManager.tokenList) {
								out.add("  " + token.id + ": " + getStatisticsEntry("users."+aboutPlayer+".selledtokens."+token.id));
							}
							
							//user sell price
							out.add(aboutPlayer+"s sell price:");
							out.add("  All: " + getStatisticsEntry("users."+aboutPlayer+".earnedprice.all"));
							for (final TemplateMain token : TokenManager.tokenList) {
								out.add("  " + token.id + ": " + getStatisticsEntry("users."+aboutPlayer+".earnedprice."+token.id));
							}
						
						} else {
							String[] lines = new String[] {
									"Bad argument. What do you want to see? ",
									"  /srm statistics global/userBuy/userSell <player>"
							};
							
							LangHandler.Out(sender, ChatColor.GREEN, lines);
						}
						
						LangHandler.Out(sender, ChatColor.GREEN, "CMD.STATISTICS.HEADER");
						LangHandler.Out(sender, ChatColor.GREEN, out.toArray(new String[out.size()]));
					}
				}
			} else {
				LangHandler.Out(sender, ChatColor.GREEN, "CMD.STATISTCS.NOT_ENABLED");
			}
			
		} else if (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("gs")) {
			
			if(isConsole) {
				LangHandler.Out(sender, ChatColor.GREEN, "Sorry, but your console don't have teleporter 3000 :D");
				return false;
			}
			
			String world = null;
			String region = null;
			
			if(args.length > 1) {
				if(args[0].equalsIgnoreCase("gs")) {
					//find first empty region by type
					for(TemplateMain token : TokenManager.tokenList) {
						if(token.id.equalsIgnoreCase(args[1])) {
							HashMap<String, HashMap<String, Object>> m = token.entries.get(player.getWorld().getName());
							if(m != null) {
								for(String r : m.keySet()) {
									if(m.get(r).get("hidden") == null || (Boolean)m.get(r).get("hidden") != true) {
										world = player.getWorld().getName();
										region = r;
										break;
									}
								}
							}
						}
					}
					if(region == null) {
						LangHandler.Out(sender, ChatColor.GREEN, "CMD.TP.NO_FREE_REGION");
					} else {
						Utils.teleportToRegion(world, region, player, true);
					}
				} else if (SimpleRegionMarket.permManager.canPlayerCommandTeleportOwned(player)) {
					//tp to your region
					for(TemplateMain t : TokenManager.tokenList) {
						for (final String w : t.entries.keySet()) {
							for (final String r : t.entries.get(w).keySet()) {
								if(r.equalsIgnoreCase(args[1])) {
									if(Utils.getEntryBoolean(t, w, r, "taken") == true && Utils.getEntryString(t, w, r, "owner").equals(player.getName())) {
										world = w;
										region = r;
										break;
									}
								}
							}
						}
					}
					if(region == null) {
						ArrayList<String> list = new ArrayList<String>();
						list.add(args[1]);
						LangHandler.ListOut(player, "CMD.TP.NOT_OWN", list);
					} else {
						Utils.teleportToRegion(world, region, player, false);
					}
				}
			} else {
				//find first empty region (only sell)
				if(args[0].equalsIgnoreCase("gs") && SimpleRegionMarket.permManager.canPlayerCommandTeleportFree(player)) {
					HashMap<String, HashMap<String, Object>> m = TokenManager.tokenList.get(0).entries.get(player.getWorld().getName());
					if(m != null)
					for(String r : m.keySet()) {
						if(m.get(r).get("hidden") == null || (Boolean)m.get(r).get("hidden") != true) {
							world = player.getWorld().getName();
							region = r;
							break;
						}
					}
					if(SimpleRegionMarket.permManager.canPlayerCommandTeleportFree(player)) {
						if(region == null) {
							LangHandler.Out(sender, ChatColor.GREEN, "CMD.TP.NO_FREE_REGION");
						} else {
							Utils.teleportToRegion(world, region, player, true);
						}
					}
				} else {
					return false;
				}
			}
		} else if (args[0].equalsIgnoreCase("money") || args[0].equalsIgnoreCase("moneys")) {
			
			if(SimpleRegionMarket.configurationHandler.getBoolean("Statistics_Enabled")) {
			
				if(isConsole || SimpleRegionMarket.permManager.canPlayerCommandAbout(player)) {
					
					String aboutPlayer = player.getName();			
					int page = 1;
					
					for(String arg : args) {
						if(!arg.equals("moneys")) {
							if(Character.isDigit(arg.charAt(0))) {
								if(Integer.parseInt(arg) > 0) {
									page = Integer.parseInt(arg);
								}
							} else {
								if(isConsole || SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
									if(args.length > 3) {
										aboutPlayer = arg;
									}
								}
							}
						}
					}
					
					LangHandler.Out(sender, ChatColor.GREEN, "CMD.MONEYS.HEADER");
					List<String> out = SimpleRegionMarket.statisticManager.getLetStatistic(aboutPlayer, page);
					if(out.size() == 0) {
						LangHandler.Out(sender, ChatColor.GREEN, "No any lets found.");
					} else {
						LangHandler.Out(sender, ChatColor.GREEN, out.toArray(new String[out.size()]));
					}	
				}				 
			} else {
				LangHandler.Out(sender, ChatColor.GREEN, "CMD.STATISTCS.NOT_ENABLED");
			}
			
		} else if (args[0].equalsIgnoreCase("list")) {
			
			if(isConsole || SimpleRegionMarket.permManager.canPlayerCommandList(player)){

				final int limit = SimpleRegionMarket.configurationHandler.getConfig().getInt("Entries_Per_List_Page", 10);
				ArrayList<String> regionList = new ArrayList<String>();
				Integer page = 1;
				String t = null;
				HashMap<String, String> data = new HashMap<String, String>();
				Location location = null;
				
				if(args.length > 1) {
					for(String arg : args) {
						if(!arg.equals("list")) {
							if(arg.contains(":")) {
								String[] s = arg.split(":");
								if(s.length == 2 && s[0] != null && s[1] != null) {
									data.put(s[0], s[1]);
								}
							} else {
								if(Character.isDigit(arg.charAt(0))) {
									if(Integer.parseInt(arg) > 0) {
										page = Integer.parseInt(arg);
									}
								} else {
									t = arg;
								}
							}
						}
					}
				}
				
				if(player != null) {
					location = player.getLocation();
				}
				
				if(t == null) {
					for (final TemplateMain token : TokenManager.tokenList) {
						for (final String world : token.entries.keySet()) {
							regionList.addAll(tokenManager.getRegionList(page, limit, token, world, data, location));
						}
					}
				} else {
					for (final TemplateMain token : TokenManager.tokenList) {
						if(token.id.equalsIgnoreCase(t)) {
							for (final String world : token.entries.keySet()) {
								regionList.addAll(tokenManager.getRegionList(page, limit, token, world, data, location));
							}
						}
					}
				}
	
				final ArrayList<String> list = new ArrayList<String>();
				list.add(String.valueOf(page));
				if(player != null) {
					LangHandler.NormalOut(player, "CMD.LIST.HEADER", list);
				} else {
					LangHandler.consoleOut("CMD.LIST.HEADER", Level.INFO, list);
				}
				LangHandler.Out(sender, ChatColor.GREEN, regionList.toArray(new String[regionList.size()]));
			}
			
		} else if (args[0].toLowerCase().equals("addmember")) {
			
			if(isConsole || SimpleRegionMarket.permManager.canPlayerCommandAddMember(player)){
				if(isConsole && args.length < 4) {
					LangHandler.consoleOut("CMD.ADD_MEMBER.NO_ARG", Level.INFO, null);
					return true;
				}
				
				String member = null;
				String region = null;
				String world = null;
				if(player != null) {
					world = player.getWorld().getName();
				}
				
				if(args.length >= 2) {
					member = args[1];
				}
				if(args.length >= 3) {
					region = args[2];
				}
				if(args.length >= 4) {
					world = args[3];
				}
				ProtectedRegion pr = Utils.getProtectedRegion(region, player.getLocation(), world);
				if(pr != null) {
					boolean isOwner = false;
					for(TemplateMain token : TokenManager.tokenList) {
						if(token.isRegionOwner(player, world, region)) {
							isOwner = true;
						}
					}
					if(isOwner) {
						if(!pr.getMembers().contains(member)) {
							pr.getMembers().addPlayer(member);
							LangHandler.Out(sender, ChatColor.GREEN, "CMD.ADD_MEMBER.SUCCESS");
							
							Player me = Bukkit.getPlayer("member");
							if(me != null) {
								LangHandler.Out(me, ChatColor.GREEN, "CMD.ADD_MEMBER.SUCCESS_MEMBER");
							}
						}
					} else {
						LangHandler.Out(sender, ChatColor.RED, "PLAYER.ERROR.NOT_OWNER");
					}
				} else {
					ArrayList<String> list = new ArrayList<String>();
					list.add(region); list.add(world);
					LangHandler.NormalOut(player, "COMMON.NO_REGION", list);
				}
			}
		} else if (args[0].toLowerCase().equals("remmember") || args[0].toLowerCase().equals("removemember")) {
			
			if(isConsole || SimpleRegionMarket.permManager.canPlayerCommandAddMember(player)){
				if(isConsole && args.length < 4) {
					LangHandler.consoleOut("CMD.REM_MEMBER.NO_ARG", Level.INFO, null);
					return true;
				}
				
				String member = null;
				String region = null;
				String world = null;
				if(player != null) {
					world = player.getWorld().getName();
				}
				
				if(args.length >= 2) {
					member = args[1];
				}
				if(args.length >= 3) {
					region = args[2];
				}
				if(args.length >= 4) {
					world = args[3];
				}
				ProtectedRegion pr = Utils.getProtectedRegion(region, player.getLocation(), world);
				if(pr != null) {
					boolean isOwner = false;
					for(TemplateMain token : TokenManager.tokenList) {
						if(token.isRegionOwner(player, world, region)) {
							isOwner = true;
						}
					}
					if(isOwner) {
						if(sender.getName().equals(member)) {
							LangHandler.Out(sender, ChatColor.GREEN, "Realy good idea :D (you lost your region...)");
						} else {
							if(pr.getMembers().contains(member)) {
								pr.getMembers().removePlayer(member);
								LangHandler.Out(sender, ChatColor.GREEN, "CMD.REM_MEMBER.SUCCESS");
								
								Player me = Bukkit.getPlayer("member");
								if(me != null) {
									LangHandler.Out(me, ChatColor.GREEN, "CMD.ADD_MEMBER.SUCCESS_MEMBER");
								}
							}
						}
					} else {
						LangHandler.Out(sender, ChatColor.RED, "PLAYER.ERROR.NOT_OWNER");
					}
				} else {
					ArrayList<String> list = new ArrayList<String>();
					list.add(region); list.add(world);
					LangHandler.NormalOut(player, "COMMON.NO_REGION", list);
				}
			}
	
		} else if (args[0].equalsIgnoreCase("limits") || args[0].equalsIgnoreCase("limit")) {
			LangHandler.Out(sender, ChatColor.GREEN, "Your limits:");
			String[] groups = SimpleRegionMarket.permManager.getPlayerGroups(player);
			for(String key : SimpleRegionMarket.limitHandler.checkPermsForList(player, groups, TokenManager.tokenList.get(0))) {
				LangHandler.Out(sender, ChatColor.GREEN, "  "+key);
			}

		} else return false;
	return true;
	}
	
	private Integer getStatisticsEntry(String path) {
		int count = 0;
		for(World world : Bukkit.getWorlds()) {
			count += SimpleRegionMarket.statisticManager.getEntry(world.getName() + "." + path);
		}
		//locked (after limits reload)
		for(World world : Bukkit.getWorlds()) {
			count += SimpleRegionMarket.statisticManager.getEntry("lock."+world.getName() + "." + path);
		}
		return count;
	}
}
