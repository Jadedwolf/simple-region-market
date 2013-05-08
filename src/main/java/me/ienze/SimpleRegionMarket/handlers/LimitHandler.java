package me.ienze.SimpleRegionMarket.handlers;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import me.ienze.SimpleRegionMarket.SimpleRegionMarket;
import me.ienze.SimpleRegionMarket.TokenManager;
import me.ienze.SimpleRegionMarket.Utils;
import me.ienze.SimpleRegionMarket.signs.TemplateMain;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class LimitHandler {

    private final static String LIMITS_NAME = "limits.yml";
    private static Configuration config = null;
    private Boolean StatisticsCountMode;

    public LimitHandler(SimpleRegionMarket plugin) {
        File f = new File(SimpleRegionMarket.getPluginDir() + LIMITS_NAME);
        if (!f.exists()) {
            plugin.saveResource("limits.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(f);

        StatisticsCountMode = SimpleRegionMarket.configurationHandler.getBoolean("Statistics_Limit_Mode");
    }

    //(auto) reloading
    public void reloadLimits() {
        SimpleRegionMarket.statisticManager.lock();
        SimpleRegionMarket.statisticManager.save();
    }

    public boolean autoClear() {
        String autoClearTime = config.getString("autoClearTime");
        Long clearTime;
        //need parse
        if (autoClearTime == null || autoClearTime.isEmpty()) {
            clearTime = 0L;
        } else {
            if (autoClearTime.contains("d") || autoClearTime.contains("h") || autoClearTime.contains("m")) {
                clearTime = Utils.parseSignTime(autoClearTime);
                config.set("autoClearTime", clearTime);
            } else {
                clearTime = Long.valueOf(autoClearTime);
            }
        }

        if (clearTime > 0) {
            Long lct;
            if (config.get("lastClearTime") == null) {
                lct = 0L;
            } else {
                lct = Long.valueOf(config.getString("lastClearTime"));
            }
            if (lct + clearTime < System.currentTimeMillis()) {
                reloadLimits();
                config.set("lastClearTime", System.currentTimeMillis());
                return true;
            }
        }

        return false;
    }

    public boolean checkPerms(Player player, String[] groups, TemplateMain token, String message) {
        for (String key : config.getKeys(true)) {
            if (key.equals("lastClearTime") || key.equals("autoClearTime")) {
                continue;
            }
            if (key == null || key.isEmpty() || config.getString(key) == null || config.getString(key).isEmpty() || config.getConfigurationSection(key) != null) {
                continue;
            }
            String[] keyParts = key.split("\\.");
            String m = checkParts(key, keyParts, player, groups, token);
            if (m != null) {
                message = m;
                return false;
            }
        }
        return true;
    }

    public String[] checkPermsForList(Player player, String[] groups, TemplateMain token) {
        ArrayList<String> list = new ArrayList<String>();
        for (String key : config.getKeys(true)) {
            if (key.equals("lastClearTime") || key.equals("autoClearTime")) {
                continue;
            }
            if (key == null || key.isEmpty() || config.getString(key) == null || config.getString(key).isEmpty() || config.getConfigurationSection(key) != null) {
                continue;
            }
            String[] keyParts = key.split("\\.");
            String m = checkParts(key, keyParts, player, groups, token);
            if (m == null) {
                int i = getLimit(key);
                if (i >= 0) {
                    list.add(key + ": " + i);
                }
            }
        }
        String[] l = new String[list.size()];
        list.toArray(l);
        return l;
    }

    public String checkParts(String fullKey, String[] keyParts, Player player, String[] groups, TemplateMain token) {
        if (keyParts.length > 0) {
            if (keyParts[0].equals("global")) {
                if (keyParts.length > 1) {
                    return checkParts(fullKey, Arrays.copyOfRange(keyParts, 1, keyParts.length), player, groups, token);
                } else {
                    if (getLimit(fullKey) >= 0 && countPlayerGlobalRegions(player) >= getLimit(fullKey)) {
                        return "PLAYER.LIMITS.GLOBAL";
                    }
                }
            } else if (keyParts[0].equals("worlds")) {
                String world = player.getWorld().getName();
                if (world.equals(keyParts[1])) {
                    if (keyParts.length > 2) {
                        return checkParts(fullKey, Arrays.copyOfRange(keyParts, 2, keyParts.length), player, groups, token);
                    } else {
                        if (getLimit(fullKey) >= 0 && countPlayerGlobalWorldRegions(player, world) >= getLimit(fullKey)) {
                            return "PLAYER.LIMITS.GLOBAL_WORLD";
                        }
                    }
                }
            } else if (keyParts[0].equals("signs")) {
                String sign = token.id;
                if (sign.equals(keyParts[1])) {
                    if (keyParts.length > 2) {
                        return checkParts(fullKey, Arrays.copyOfRange(keyParts, 2, keyParts.length), player, groups, token);
                    } else {
                        if (fullKey.contains("worlds")) {
                            String world = player.getWorld().getName();
                            if (getLimit(fullKey) >= 0 && countPlayerWorldRegions(player, token, world) >= getLimit(fullKey)) {
                                return "PLAYER.LIMITS.TOKEN_WORLD";
                            }
                        } else {
                            if (getLimit(fullKey) >= 0 && countPlayerTokenRegions(player, token) >= getLimit(fullKey)) {
                                return "PLAYER.LIMITS.TOKEN";
                            }
                        }
                    }
                }
            } else if (keyParts[0].equals("groups")) {
                for (String group : groups) {
                    if (group.equals(keyParts[1])) {
                        if (keyParts.length > 2) {
                            return checkParts(fullKey, Arrays.copyOfRange(keyParts, 2, keyParts.length), player, groups, token);
                        } else {
                            if (fullKey.contains("worlds")) {
                                String world = player.getWorld().getName();
                                if (getLimit(fullKey) >= 0 && countPlayerWorldRegions(player, token, world) >= getLimit(fullKey)) {
                                    return "PLAYER.LIMITS.GROUP_WORLD";
                                }
                            } else if (fullKey.contains("signs")) {
                                if (getLimit(fullKey) >= 0 && countPlayerTokenRegions(player, token) >= getLimit(fullKey)) {
                                    return "PLAYER.LIMITS.GROUP_TOKEN";
                                }
                            } else {
                                if (getLimit(fullKey) >= 0 && countPlayerGlobalRegions(player) >= getLimit(fullKey)) {
                                    return "PLAYER.LIMITS.GROUP";
                                }
                            }
                        }
                    }
                }
            } else {
                if (keyParts.length > 0) {
                    return checkParts(fullKey, Arrays.copyOfRange(keyParts, 1, keyParts.length), player, groups, token);
                } else {
                    LangHandler.consoleOut("Incorrect limits key, removing.", Level.WARNING, null);
                    config.set(fullKey, null);
                }
            }
        }
        return null;
    }

    private int getLimit(String a) {
        Integer i = config.getInt(a);
        if (i != null) {
            return i;
        }
        return -1;
    }

    /**
     * Returns a count from template, parentRegion for regions, where player is
     * owner
     *
     * @param player
     * @param token template, where to count regions from
     * @param parentRegion
     * @return the count of all regions from the template in the parent region
     * owned by the player
     */
    public int countPlayerChildRegions(Player player, TemplateMain token, ProtectedRegion parentRegion) {
        int count = 0;
        for (final String world : token.entries.keySet()) {
            for (final String region : token.entries.get(world).keySet()) {
                final ProtectedRegion childRegion = SimpleRegionMarket.wgManager.getProtectedRegion(Bukkit.getWorld(world), region);
                if (childRegion != null && childRegion.getParent().equals(parentRegion)) {
                    if (token.isRegionOwner(player, world, region)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Returns a global count for regions with the parent region parentRegion,
     * where player is owner
     *
     * @param player
     * @param parentRegion
     * @return the count of all regions with the parent region owned by the
     * player
     */
    public int countPlayerGlobalChildRegions(Player player, ProtectedRegion parentRegion) {
        int count = 0;
        for (final TemplateMain token : TokenManager.tokenList) {
            count += countPlayerChildRegions(player, token, parentRegion);
        }
        return count;
    }

    /**
     * Returns a count from template, world for regions, where player is owner
     *
     * @param player
     * @param token template, where to count regions from
     * @param world
     * @return the count of all regions from the template in the world owned by
     * the player
     */
    public int countPlayerWorldRegions(Player player, TemplateMain token, String world) {
        int count = 0;
        if (StatisticsCountMode) {
            count = SimpleRegionMarket.statisticManager.getEntry(world + ".users." + player.getName() + ".buyedtokens." + token.id);
        } else {
            if (token.entries.containsKey(world)) {
                for (final String region : token.entries.get(world).keySet()) {
                    final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(Bukkit.getWorld(world), region);
                    if (protectedRegion != null) {
                        if (token.isRegionOwner(player, world, region)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    /**
     * Returns a global count for regions in world, where player is owner
     *
     * @param player
     * @param world
     * @return the count of all regions in the world owned by the player
     */
    public int countPlayerGlobalWorldRegions(Player player, String world) {
        int count = 0;
        for (final TemplateMain token : TokenManager.tokenList) {
            count += countPlayerWorldRegions(player, token, world);
        }
        return count;
    }

    /**
     * Returns a count per template for regions, where player is owner
     *
     * @param player
     * @param token template, where to count regions from
     * @return the count of all regions from the template owned by the player
     */
    public int countPlayerTokenRegions(Player player, TemplateMain token) {
        int count = 0;
        if (StatisticsCountMode) {
            for (World world : Bukkit.getWorlds()) {
                count += SimpleRegionMarket.statisticManager.getEntry(world.getName() + ".users." + player.getName() + ".buyedtokens." + token.id);
            }
        } else {
            for (final String world : token.entries.keySet()) {
                for (final String region : token.entries.get(world).keySet()) {
                    final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(Bukkit.getWorld(world), region);
                    if (protectedRegion != null) {
                        if (token.isRegionOwner(player, world, region)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    /**
     * Returns a global count for regions, where player is owner
     *
     * @param player
     * @return the count of all regions owned by the player
     */
    public int countPlayerGlobalRegions(Player player) {
        int count = 0;
        for (final TemplateMain token : TokenManager.tokenList) {
            count += countPlayerTokenRegions(player, token);
        }
        return count;
    }
}
