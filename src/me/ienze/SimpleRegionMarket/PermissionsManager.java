package me.ienze.SimpleRegionMarket;

import me.ienze.SimpleRegionMarket.handlers.LangHandler;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class PermissionsManager {

	public Permission permission;
	private boolean loadedPermissions;

	public boolean loadPermissionSystem(SimpleRegionMarket plugin){
        RegisteredServiceProvider<Permission> permissionProvider = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
         
        if (permission != null && permission.isEnabled() && !permission.getName().equals("SuperPerms")) {
        	loadedPermissions = true;
        	return true;
        }
        return false;
    }
	
	//signs
	public boolean canPlayerUseSign(Player player, String sign) {
		return playerHasPerm (player, "simpleregionmarket.signs.use."+sign);
	}
	
	public boolean canPlayerCreateSign(Player player, String sign) {
		return playerHasPerm (player, "simpleregionmarket.signs.create."+sign);
	}
	
	public boolean canPlayerCommandList(Player player) {
		return playerHasPerm (player, "simpleregionmarket.list");
	}
	
	public boolean canPlayerCommandLimits(Player player) {
		return playerHasPerm (player, "simpleregionmarket.limits");
	}
	
	public boolean canPlayerCommandAbout(Player player) {
		return playerHasPerm (player, "simpleregionmarket.about");
	}

	public boolean canPlayerCommandTeleportFree(Player player) {
		return playerHasPerm (player, "simpleregionmarket.tpfree");
	}
	
	public boolean canPlayerCommandTeleportOwned(Player player) {
		return playerHasPerm (player, "simpleregionmarket.tpowned");
	}
	
	public boolean canPlayerCommandAddMember(Player player) {
		return playerHasPerm (player, "simpleregionmarket.addmember");
	}
	
	public boolean canPlayerCommandRemoveMember(Player player) {
		return playerHasPerm (player, "simpleregionmarket.removemember");
	}

	//admin perms
	public boolean hadAdminPermissions(Player player) {
		if(loadedPermissions) {
			return permission.has(player, "simpleregionmarket.admin");
		} else {
			return player.hasPermission("simpleregionmarket.admin");
		}
	}
	
	private boolean playerHasPerm (Player player, String perm) {

		if(loadedPermissions) {
			if(permission.has(player, perm) || permission.has(player, "simpleregionmarket.admin")) {
				return true;
			} else {
				LangHandler.ErrorOut(player, "PLAYER.NO_PERMISSIONS", null);
			}
		} else {
			if(player.hasPermission(perm) || player.hasPermission("simpleregionmarket.admin")) {
				return true;
			} else {
				LangHandler.ErrorOut(player, "PLAYER.NO_PERMISSIONS", null);
			}
		}
		return false;
	}
	
	public String[] getPlayerGroups(Player player) {
		if(loadedPermissions) {
			return permission.getPlayerGroups(player);
		}
		return new String[] {};
	}
	
	public String[] getGroups() {
		if(loadedPermissions) {
			return permission.getGroups();
		}
		return new String[] {};
	}
}
