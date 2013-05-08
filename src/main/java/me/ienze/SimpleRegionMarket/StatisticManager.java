package me.ienze.SimpleRegionMarket;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.ienze.SimpleRegionMarket.signs.TemplateMain;
import org.bukkit.configuration.file.YamlConfiguration;

public class StatisticManager {

    private static boolean enabled = false;
    public final static String CONFIG_NAME = "statistics.yml";
    public final static File CONFIG_FILE = new File(SimpleRegionMarket.getPluginDir() + CONFIG_NAME);
    private YamlConfiguration configHandle;
    private String LetStatisticsTemplate;

    public StatisticManager() {
        enabled = SimpleRegionMarket.configurationHandler.getBoolean("Statistics_Enabled");

        if (enabled) {
            if (CONFIG_FILE.exists()) {
                configHandle = YamlConfiguration.loadConfiguration(CONFIG_FILE);
            } else {
                try {
                    CONFIG_FILE.createNewFile();
                    configHandle = YamlConfiguration.loadConfiguration(CONFIG_FILE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            LetStatisticsTemplate = SimpleRegionMarket.configurationHandler.getString("Let_Statistics_Template");
        }
    }

    public void save() {
        try {
            if (enabled) {
                configHandle.save(CONFIG_FILE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        if (enabled) {
            if (CONFIG_FILE.exists()) {
                CONFIG_FILE.delete();
                configHandle = YamlConfiguration.loadConfiguration(CONFIG_FILE);
            }
        }
    }

    public void lock() {
        if (enabled) {
            for (String key : configHandle.getKeys(true)) {
                if (!key.startsWith("lock")) {
                    if (configHandle.getConfigurationSection("lock." + key) != null) {
                    } else {
                        Integer locked = configHandle.getInt("lock." + key);
                        if (locked != null) {
                            configHandle.set("lock." + key, configHandle.getInt(key) + locked);
                        } else {
                            configHandle.set("lock." + key, configHandle.getInt(key));
                        }
                    }
                }
            }

            for (String key : configHandle.getKeys(false)) {
                if (!key.equals("lock")) {
                    configHandle.set(key, null);
                }
            }
        }
    }

    public void setEntry(String key, Integer value) {
        configHandle.set(key, value);
    }

    public Integer getEntry(String key) {
        Integer value = configHandle.getInt(key);
        if (value != null) {
            return value;
        }
        return 0;
    }

    public void add(String[] keys) {
        for (String key : keys) {
            Integer old = getEntry(key);
            setEntry(key, old + 1);
        }
    }

    public void addValue(String[] keys, Integer value) {
        if (enabled) {
            for (int i = 0; i < keys.length; i++) {
                Integer old = getEntry(keys[i]);
                setEntry(keys[i], old + value);
            }
        }
    }

    public void onSignClick(String tokenId, String world, String playerSell, String playerBuy) {
        if (enabled) {
            //global
            add(new String[]{world + ".global.token.all", world + ".global.token." + tokenId});
            //buyer
            add(new String[]{world + ".users." + playerBuy + ".buyedtokens.all", world + ".users." + playerBuy + ".buyedtokens." + tokenId});
            //seller
            add(new String[]{world + ".users." + playerSell + ".selledtokens.all", world + ".users." + playerSell + ".selledtokens." + tokenId});
        }
    }

    public void onMoneysUse(String tokenId, String world, Double price, String playerSell, String playerBuy) {
        if (enabled) {
            //global
            addValue(new String[]{world + ".global.price.all", world + ".global.price." + tokenId}, (int) Math.round(price));
            //buyer
            addValue(new String[]{world + ".users." + playerBuy + ".payedprice.all", world + ".users." + playerBuy + ".payedprice." + tokenId}, (int) Math.round(price));
            //seller
            addValue(new String[]{world + ".users." + playerSell + ".earnedprice.all", world + ".users." + playerSell + ".earnedprice." + tokenId}, (int) Math.round(price));
        }

        save();
    }

    public List<String> getLetStatistic(String player, Integer page) {
        final int limit = SimpleRegionMarket.configurationHandler.getConfig().getInt("Entries_Per_List_Page", 10);
        ArrayList<String> out = new ArrayList<String>();
        int count = 1;
        for (final TemplateMain token : TokenManager.tokenList) {
            if (token.id.equalsIgnoreCase("let")) {
                for (final String world : token.entries.keySet()) {
                    for (final String region : token.entries.get(world).keySet()) {
                        String owner = Utils.getEntryString(token, world, region, "owner");
                        if (owner != null) {
                            if (owner.equals(player) || Utils.getEntryString(token, world, region, "account").equals(player)) {
                                if (count > (page * limit) - limit && count < (page * limit)) {
                                    String label = LetStatisticsTemplate;
                                    label = label.replace("&playerFrom&", Utils.getEntryString(token, world, region, "owner"));
                                    label = label.replace("&playerTo&", Utils.getEntryString(token, world, region, "account"));
                                    label = label.replace("&price&", Utils.getEntryString(token, world, region, "price"));
                                    label = label.replace("&time&", Utils.getSignTime(Utils.getEntryLong(token, world, region, "renttime")));
                                    out.add(label);
                                }
                                count++;
                            }
                        }
                    }
                }
            }
        }
        List<String> newList = Arrays.asList(out.toArray(new String[out.size()]));
        return newList;
    }
}