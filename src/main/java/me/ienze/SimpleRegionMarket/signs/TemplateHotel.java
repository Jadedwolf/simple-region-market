package me.ienze.SimpleRegionMarket.signs;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.ienze.SimpleRegionMarket.SimpleRegionMarket;
import me.ienze.SimpleRegionMarket.TokenManager;
import me.ienze.SimpleRegionMarket.Utils;
import me.ienze.SimpleRegionMarket.handlers.LangHandler;

/**
 * @author theZorro266
 * 
 */
public class TemplateHotel extends TemplateLet {
	public TemplateHotel(SimpleRegionMarket plugin, TokenManager tokenManager, String tplId) {
		super(plugin, tokenManager, tplId);
	}
	
	@Override
	public void otherClicksSign(Player player, String world, String region) {
		if(SimpleRegionMarket.permManager.canPlayerUseSign(player, "rent")) {
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
				String account = Utils.getEntryString(this, world, region, "account");
				SimpleRegionMarket.statisticManager.onSignClick(this.id, world, account, player.getName());
				takeRegion(player, world, region);
			}
		}
	}
	
	@Override
	public void ownerClicksTakenSign(String world, String region) {
		final long newRentTime = Utils.getEntryLong(this, world, region, "expiredate") + Utils.getEntryLong(this, world, region, "renttime");
		final Player owner = Bukkit.getPlayer(Utils.getEntryString(this, world, region, "owner"));
		Boolean rentLimit;
		String mre = SimpleRegionMarket.configurationHandler.getString("Max_Rent_Extend");
		if(mre.contains("d") || mre.contains("h") || mre.contains("m")) {
			rentLimit = (newRentTime - System.currentTimeMillis()) < Utils.parseSignTime(mre);
		} else {
			rentLimit = (newRentTime - System.currentTimeMillis()) / Utils.getEntryDouble(this, world, region, "renttime") < Integer.valueOf(mre);
		}
		if (mre.equals("-1") || mre.equals("0") || rentLimit) {
			if(SimpleRegionMarket.configurationHandler.getBoolean("Can_Extend")) {
				if (SimpleRegionMarket.econManager.isEconomy()) {
					String account = Utils.getEntryString(this, world, region, "account");
					if (account.isEmpty()) {
						account = null;
					}
					final double price = Utils.getEntryDouble(this, world, region, "price");
					if (SimpleRegionMarket.econManager.moneyTransaction(Utils.getEntryString(this, world, region, "owner"), account, price)) {
						Utils.setEntry(this, world, region, "expiredate", newRentTime);
						tokenManager.updateSigns(this, world, region);
						LangHandler.NormalOut(owner, "PLAYER.REGION.ADDED_RENTTIME", null);
						SimpleRegionMarket.statisticManager.onMoneysUse(this.id, world, price, account, owner.getName());
					}
				} else {
					Utils.setEntry(this, world, region, "expiredate", newRentTime);
					tokenManager.updateSigns(this, world, region);
					LangHandler.NormalOut(owner, "PLAYER.REGION.ADDED_RENTTIME", null);
				}
			}
		} else {
			LangHandler.ErrorOut(owner, "PLAYER.ERROR.RERENT_TOO_LONG", null);
		}
	}

	@Override
	public void schedule(String world, String region) {
		if (Utils.getEntryBoolean(this, world, region, "taken")) {
			if (Utils.getEntryLong(this, world, region, "expiredate") < System.currentTimeMillis()) {
				if (Utils.getEntry(this, world, region, "owner") != null) {
					final Player player = Bukkit.getPlayer(Utils.getEntryString(this, world, region, "owner"));
					if (player != null) {
						final ArrayList<String> list = new ArrayList<String>();
						list.add(region);
						LangHandler.NormalOut(player, "PLAYER.REGION.EXPIRED", list);
					}
				}
				untakeRegion(world, region);
			} else {
				//warning 5 and minute before.
				final String owner = Utils.getEntryString(this, world, region, "owner");
				final Player player = Bukkit.getPlayer(owner);
				if(player != null) {
					String warns = SimpleRegionMarket.configurationHandler.getConfig().getString("Show_Warning_Rent_Expire", "0");
					for(String warn : warns.split(",")) {
						if(Integer.valueOf(warn) != 0) {
							if ((Utils.getEntryLong(this, world, region, "expiredate") - System.currentTimeMillis())/(1000*60) == Integer.valueOf(warn)) {
								final ArrayList<String> list = new ArrayList<String>();
								list.add(region);
								list.add(warn);
								LangHandler.NormalOut(player, "PLAYER.REGION.WARN_EXPIRE", list);
							}
						}
					}
				}
			}
		}
	}
}
