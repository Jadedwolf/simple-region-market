package me.ienze.SimpleRegionMarket.signs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.ienze.SimpleRegionMarket.SimpleRegionMarket;
import me.ienze.SimpleRegionMarket.TokenManager;
import me.ienze.SimpleRegionMarket.Utils;
import me.ienze.SimpleRegionMarket.handlers.LangHandler;

/**
 * @author theZorro266
 * 
 */
public class TemplateLet extends TemplateMain {
	public TemplateLet(SimpleRegionMarket plugin, TokenManager tokenManager, String tplId) {
		super(plugin,  tokenManager);
		id = tplId;

		canLiveWithoutSigns = false;

		load();
	}

	@Override
	public void otherClicksSign(Player player, String world, String region) {
		if(SimpleRegionMarket.permManager.canPlayerUseSign(player, "let")) {
			if (SimpleRegionMarket.econManager.isEconomy()) {
				String account = Utils.getEntryString(this, world, region, "account");
				if (account.isEmpty()) {
					account = null;
				}
				final double price = Utils.getEntryDouble(this, world, region, "price");
				if (SimpleRegionMarket.econManager.moneyTransaction(player.getName(), account, price)) {
					takeRegion(player, world, region);
					SimpleRegionMarket.statisticManager.onSignClick(this.id, world, account, player.getName());
					SimpleRegionMarket.statisticManager.onMoneysUse(this.id, world, price, account, player.getName());
				}
			} else {
				takeRegion(player, world, region);
			}
		}
	}
	
	@Override
	public void takeRegion(Player newOwner, String world, String region) {
		final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(Bukkit.getWorld(world), region);

		if (Utils.getEntryBoolean(this, world, region, "taken")) {
			final Player oldOwner = Bukkit.getPlayer(Utils.getEntryString(this, world, region, "owner"));
			final ArrayList<String> list = new ArrayList<String>();
			list.add(region);
			list.add(newOwner.getName());
			LangHandler.NormalOut(oldOwner, "PLAYER.REGION.JUST_TAKEN_BY", list);
			untakeRegion(world, region);
		} else {
			// Clear Members
			protectedRegion.setMembers(new DefaultDomain());
		}
		
		protectedRegion.getMembers().addPlayer(SimpleRegionMarket.wgManager.wrapPlayer(newOwner));
		
		checkTakeActions(protectedRegion, world);
		
		Utils.setEntry(this, world, region, "taken", true);
		Utils.setEntry(this, world, region, "owner", newOwner.getName());
		Utils.setEntry(this, world, region, "expiredate", System.currentTimeMillis() + Utils.getEntryLong(this, world, region, "renttime"));
		Utils.setEntry(this, world, region, "hidden", true);
		
		final ArrayList<String> list = new ArrayList<String>();
		list.add(region);
		LangHandler.NormalOut(newOwner, "PLAYER.REGION.RENT", list);

		tokenManager.updateSigns(this, world, region);
	}

	@Override
	public void untakeRegion(String world, String region) {
		final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(Bukkit.getWorld(world), region);

		// Clear Members
		protectedRegion.setMembers(new DefaultDomain());

		checkUntakeActions(protectedRegion, world);
		
		Utils.setEntry(this, world, region, "taken", false);
		Utils.removeEntry(this, world, region, "owner");
		Utils.removeEntry(this, world, region, "expiredate");
		Utils.setEntry(this, world, region, "hidden", false);
		
		tokenManager.updateSigns(this, world, region);
	}

	public void ownerClicksTakenSign(String world, String region) {
		untakeRegion(world, region);
	}
	
	@Override
	public boolean signCreated(Player player, String world, ProtectedRegion protectedRegion, Location signLocation, HashMap<String, String> input,
			String[] lines) {
		final String region = protectedRegion.getId();

		if (!entries.containsKey(world) || !entries.get(world).containsKey(region)) {
			final double priceMin = Utils.getOptionDouble(this, "price.min");
			final double priceMax = Utils.getOptionDouble(this, "price.max");
			double price;
			if (SimpleRegionMarket.econManager.isEconomy()) {
				if (input.get("price") != null) {
					try {
						price = Double.parseDouble(input.get("price"));
					} catch (final Exception e) {
						LangHandler.ErrorOut(player, "PLAYER.ERROR.NO_PRICE", null);
						return false;
					}
				} else {
					price = priceMin;
				}
			} else {
				price = 0;
			}

			if (priceMin > price && (priceMax == -1 || price < priceMax)) {
				final ArrayList<String> list = new ArrayList<String>();
				list.add(String.valueOf(priceMin));
				list.add(String.valueOf(priceMax));
				LangHandler.ErrorOut(player, "PLAYER.ERROR.PRICE_LIMIT", list);
				return false;
			}

			final long renttimeMin = Utils.getOptionLong(this, "renttime.min");
			final long renttimeMax = Utils.getOptionLong(this, "renttime.max");
			long renttime;
			if (input.get("time") != null && !input.get("time").isEmpty()) {
				try {
					renttime = Utils.parseSignTime(input.get("time"));
				} catch (final Exception e) {
					LangHandler.ErrorOut(player, "PLAYER.ERROR.NO_RENTTIME", null);
					return false;
				}
			} else {
				LangHandler.ErrorOut(player, "PLAYER.ERROR.NO_RENTTIME", null);
				return false;
			}

			if (renttimeMin > renttime && (renttimeMax == -1 || renttime < renttimeMax)) {
				final ArrayList<String> list = new ArrayList<String>();
				list.add(String.valueOf(renttimeMin));
				list.add(String.valueOf(renttimeMax));
				LangHandler.ErrorOut(player, "PLAYER.ERROR.RENTTIME_LIMIT", list);
				return false;
			}
			
			String account = "";
			if (input.get("account") != null && !input.get("account").isEmpty()) {
				account = input.get("account");
				if (SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
					if (input.get("account").equalsIgnoreCase("none")) {
						account = "";
					}
				}
			} else {
				if(SimpleRegionMarket.configurationHandler.getBoolean("Player_Line_Empty")) {
					account = player.getName();
				} else {
					account = SimpleRegionMarket.configurationHandler.getString("Default_Economy_Account");
				}
			}

			Utils.setEntry(this, world, region, "price", price);
			Utils.setEntry(this, world, region, "renttime", renttime);
			Utils.setEntry(this, world, region, "account", account);
			Utils.setEntry(this, world, region, "taken", false);
			Utils.removeEntry(this, world, region, "owner");
		}

		final ArrayList<Location> signLocations = Utils.getSignLocations(this, world, region);
		signLocations.add(signLocation);
		if (signLocations.size() == 1) {
			Utils.setEntry(this, world, region, "signs", signLocations);
		}

		tokenManager.updateSigns(this, world, region);
		return true;
	}

	@Override
	public Map<String, String> getReplacementMap(String world, String region) {
		final HashMap<String, String> replacementMap = (HashMap<String, String>) super.getReplacementMap(world, region);
		if (replacementMap != null) {
			replacementMap.put("time", Utils.getSignTime(Utils.getEntryLong(this, world, region, "renttime")));
			if (Utils.getEntry(this, world, region, "expiredate") != null) {
				replacementMap.put("timeleft", Utils.getSignTime(Utils.getEntryLong(this, world, region, "expiredate") - System.currentTimeMillis()));
			}
		}
		return replacementMap;
	}

	@Override
	public void schedule(String world, String region) {
		if (Utils.getEntryBoolean(this, world, region, "taken")) {
			if (Utils.getEntryLong(this, world, region, "expiredate") < System.currentTimeMillis()) {
				if (Utils.getEntry(this, world, region, "owner") != null) {
					final String owner = Utils.getEntryString(this, world, region, "owner");
					final String account = Utils.getEntryString(this, world, region, "account");
					final Double price = Utils.getEntryDouble(this, world, region, "price");
					final Player player = Bukkit.getPlayer(owner);
					if (SimpleRegionMarket.econManager.econHasEnough(owner, price)) {
						if (SimpleRegionMarket.econManager.moneyTransaction(owner, account, price)) {
							final long newExpTime = Utils.getEntryLong(this, world, region, "expiredate") + Utils.getEntryLong(this, world, region, "renttime");
							Utils.setEntry(this, world, region, "expiredate", newExpTime);
							if (player != null) {
								if(SimpleRegionMarket.configurationHandler.getConfig().getBoolean("Show_Auto_Expand_Message", true)) {
									final ArrayList<String> list = new ArrayList<String>();
									list.add(region);
									LangHandler.NormalOut(player, "PLAYER.REGION.AUTO_EXPANDED", list);
								}
								SimpleRegionMarket.statisticManager.onMoneysUse(this.id, world, price, account, player.getName());
							}
							return;
						}
					}
					if (player != null) {
						final ArrayList<String> list = new ArrayList<String>();
						list.add(region);
						LangHandler.NormalOut(player, "PLAYER.REGION.EXPIRED", list);
					}
				}
				untakeRegion(world, region);
			}
		}
	}
}
