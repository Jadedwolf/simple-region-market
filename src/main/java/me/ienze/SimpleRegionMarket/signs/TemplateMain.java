package me.ienze.SimpleRegionMarket.signs;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import me.ienze.SimpleRegionMarket.SimpleRegionMarket;
import me.ienze.SimpleRegionMarket.TokenManager;
import me.ienze.SimpleRegionMarket.Utils;
import me.ienze.SimpleRegionMarket.handlers.LangHandler;
import me.ienze.SimpleRegionMarket.regionTasks.TerrainRegeneratorThread;
import me.ienze.SimpleRegionMarket.regions.RegionSaverData;
import me.ienze.SimpleRegionMarket.regions.SimpleRegionData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * @author theZorro266
 *
 */
public abstract class TemplateMain {

    public String id = null;
    public boolean canLiveWithoutSigns = true;
    protected final SimpleRegionMarket plugin;
    protected final TokenManager tokenManager;
    /**
     * HashMap<Key:String, Value:Object>
     */
    public HashMap<String, Object> tplOptions = new HashMap<String, Object>();
    /**
     * HashMap<World:String, HashMap<Region:String, HashMap<Key:String,
     * Value:Object>>>
     */
    public HashMap<String, HashMap<String, HashMap<String, Object>>> entries = new HashMap<String, HashMap<String, HashMap<String, Object>>>();

    public TemplateMain(SimpleRegionMarket plugin, TokenManager tokenManager) {
        this.plugin = plugin;
        this.tokenManager = tokenManager;
    }

    public void load() {
        if (checkTemplate()) {
            final YamlConfiguration configHandle = YamlConfiguration.loadConfiguration(TokenManager.CONFIG_FILE);

            for (final String key : configHandle.getConfigurationSection(id).getKeys(true)) {
                tplOptions.put(key, configHandle.getConfigurationSection(id).get(key));
            }

            final YamlConfiguration tokenLoad = YamlConfiguration.loadConfiguration(new File(SimpleRegionMarket.getPluginDir() + "signs/" + id.toLowerCase()
                    + ".yml"));

            ConfigurationSection path;
            for (final String lWorld : tokenLoad.getKeys(false)) {
                path = tokenLoad.getConfigurationSection(lWorld);
                for (final String lRegion : path.getKeys(false)) {
                    path = tokenLoad.getConfigurationSection(lWorld).getConfigurationSection(lRegion);
                    for (final String lKey : path.getKeys(false)) {
                        if (lKey.equals("signs")) {
                            final ConfigurationSection pathSigns = path.getConfigurationSection("signs");
                            final ArrayList<Location> signLocations = new ArrayList<Location>();
                            for (final String lNr : pathSigns.getKeys(false)) {
                                String world = pathSigns.getString(lNr + ".world");
                                if (world == null) {
                                    world = lWorld;
                                }
                                signLocations.add(new Location(Bukkit.getWorld(world), pathSigns.getDouble(lNr + ".X"), pathSigns.getDouble(lNr + ".Y"),
                                        pathSigns.getDouble(lNr + ".Z")));
                            }
                            Utils.setEntry(this, lWorld, lRegion, lKey, signLocations);
                        } else {
                            Utils.setEntry(this, lWorld, lRegion, lKey, path.get(lKey));
                        }
                    }
                }
            }
        } else {
            Bukkit.getLogger().log(Level.SEVERE, "[SRM] Error loading templates.");
        }
    }

    public void save() {
        if (checkTemplate()) {
            final YamlConfiguration tokenSave = new YamlConfiguration();

            for (final String string : entries.keySet()) {
                final String sWorld = string;
                for (final String string2 : entries.get(sWorld).keySet()) {
                    final String sRegion = string2;
                    for (final String string3 : entries.get(sWorld).get(sRegion).keySet()) {
                        final String sKey = string3;
                        if (sKey.equals("signs")) {
                            int counter = 0;
                            for (final Location signLoc : Utils.getSignLocations(this, sWorld, sRegion)) {
                                tokenSave.set(sWorld + "." + sRegion + ".signs." + counter + ".world", signLoc.getWorld().getName());
                                tokenSave.set(sWorld + "." + sRegion + ".signs." + counter + ".X", signLoc.getX());
                                tokenSave.set(sWorld + "." + sRegion + ".signs." + counter + ".Y", signLoc.getY());
                                tokenSave.set(sWorld + "." + sRegion + ".signs." + counter + ".Z", signLoc.getZ());
                                counter++;
                            }
                        } else {
                            tokenSave.set(sWorld + "." + sRegion + "." + sKey, Utils.getEntry(this, sWorld, sRegion, sKey));
                        }
                    }
                }
            }

            try {
                tokenSave.save(new File(SimpleRegionMarket.getPluginDir() + "signs/" + id.toLowerCase() + ".yml"));
            } catch (final IOException e) {
                Bukkit.getLogger().log(Level.SEVERE, "[SRM] Error saving token " + id + ".");
                e.printStackTrace();
            }
        }
    }

    public boolean checkTemplate() {
        if (id != null) {
            return true;
        } else {
            Bukkit.getLogger().log(Level.SEVERE, "[SRM] Template misconfiguration.");
        }
        return false;
    }

    public Map<String, String> getReplacementMap(String world, String region) {
        if (checkTemplate()) {
            if (world != null && entries.containsKey(world) && region != null && entries.get(world).containsKey(region)) {
                final World worldWorld = Bukkit.getWorld(world);
                if (worldWorld != null) {
                    final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(worldWorld, region);
                    if (protectedRegion != null) {
                        final HashMap<String, String> replacementMap = new HashMap<String, String>();
                        replacementMap.put("id", id);
                        replacementMap.put("id_out", Utils.getOptionString(this, "output.id"));
                        replacementMap.put("id_taken", Utils.getOptionString(this, "taken.id"));
                        replacementMap.put("world", world.toLowerCase());
                        replacementMap.put("region", region.toLowerCase());
                        if (!SimpleRegionMarket.econManager.isEconomy() || Utils.getEntryDouble(this, world, region, "price") == 0) {
                            replacementMap.put("price", "FREE");
                        } else {
                            replacementMap.put("price", SimpleRegionMarket.econManager.econFormat(Utils.getEntryDouble(this, world, region, "price")));
                        }
                        replacementMap.put("account", Utils.getEntryString(this, world, region, "account"));
                        if (Utils.getEntry(this, world, region, "owner") == null || Utils.getEntryString(this, world, region, "owner").isEmpty()) {
                            replacementMap.put("player", "No owner");
                        } else {
                            replacementMap.put("player", Utils.getEntryString(this, world, region, "owner"));
                        }
                        replacementMap.put("x", Integer.toString(Math.abs((int) protectedRegion.getMaximumPoint().getX()
                                - (int) (protectedRegion.getMinimumPoint().getX() - 1))));
                        replacementMap.put("y", Integer.toString(Math.abs((int) protectedRegion.getMaximumPoint().getY()
                                - (int) (protectedRegion.getMinimumPoint().getY() - 1))));
                        replacementMap.put("z", Integer.toString(Math.abs((int) protectedRegion.getMaximumPoint().getZ()
                                - (int) (protectedRegion.getMinimumPoint().getZ() - 1))));

                        return replacementMap;
                    }
                }
            }
        }
        return null;
    }

    public boolean isRegionOwner(Player player, String world, String region) {
        if (Utils.getEntry(this, world, region, "taken") != null && Utils.getEntryBoolean(this, world, region, "taken")
                && Utils.getEntry(this, world, region, "owner") != null) {
            if (Utils.getEntryString(this, world, region, "owner").equalsIgnoreCase(player.getName())) {
                return true;
            }
        }
        return false;
    }

    public void ownerClicksTakenSign(String world, String region) {
        final Player owner = Bukkit.getPlayer(Utils.getEntryString(this, world, region, "owner"));
        if (!SimpleRegionMarket.permManager.hadAdminPermissions(owner)) {
            LangHandler.NormalOut(owner, "PLAYER.REGION.YOURS", null);
        } else {
            otherClicksTakenSign(owner, world, region);
        }
    }

    public void ownerClicksSign(Player owner, String world, String region) {
        if (!SimpleRegionMarket.permManager.hadAdminPermissions(owner)) {
            LangHandler.NormalOut(owner, "PLAYER.REGION.YOURS", null);
        } else {
            otherClicksSign(owner, world, region);
        }
    }

    public void otherClicksTakenSign(Player player, String world, String region) {
        final ArrayList<String> list = new ArrayList<String>();
        list.add(Utils.getEntryString(this, world, region, "owner"));
        LangHandler.NormalOut(player, "PLAYER.REGION.TAKEN_BY", list);
    }

    public void otherClicksSign(Player player, String world, String region) {
        if (SimpleRegionMarket.permManager.canPlayerUseSign(player, "sell")) {
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
            // Clear Members and Owners
            protectedRegion.setMembers(new DefaultDomain());
            protectedRegion.setOwners(new DefaultDomain());
        }

        protectedRegion.getOwners().addPlayer(SimpleRegionMarket.wgManager.wrapPlayer(newOwner));

        checkTakeActions(protectedRegion, world);

        if (Utils.getOptionBoolean(this, "removesigns")) {
            for (final Location sign : Utils.getSignLocations(this, world, region)) {
                sign.getBlock().setType(Material.AIR);
            }
            Utils.setEntry(this, world, region, "signs", null);
        }

        Utils.setEntry(this, world, region, "taken", true);
        Utils.setEntry(this, world, region, "owner", newOwner.getName());
        Utils.setEntry(this, world, region, "hidden", true);

        final ArrayList<String> list = new ArrayList<String>();
        list.add(region);
        LangHandler.NormalOut(newOwner, "PLAYER.REGION.BOUGHT", list);

        //teleport to it
        if (SimpleRegionMarket.configurationHandler.getBoolean("Teleport_To_Region")) {
            Utils.teleportToRegion(world, region, newOwner, false);
        }
        tokenManager.updateSigns(this, world, region);

        if (SimpleRegionMarket.configurationHandler.getString("Auto_Removing_Regions").contains(this.id)) {
            Utils.setEntry(this, world, region, "hidden", true);
        }
    }

    public void untakeRegion(String world, String region) {
        final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(Bukkit.getWorld(world), region);

        // Clear Members and Owners
        protectedRegion.setMembers(new DefaultDomain());
        protectedRegion.setOwners(new DefaultDomain());

        checkUntakeActions(protectedRegion, world);

        Utils.setEntry(this, world, region, "taken", false);
        Utils.removeEntry(this, world, region, "owner");
        Utils.setEntry(this, world, region, "hidden", false);

        List<String> list = Arrays.asList(SimpleRegionMarket.configurationHandler.getString("Remove_LWC_On_Expire").split(","));
        if (list.contains(this.id)) {
            SimpleRegionMarket.lwcManager.removeRegionProtection(protectedRegion, world);
        }

        tokenManager.updateSigns(this, world, region);
    }

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

            String account = "";
            if (input.get("account") != null && !input.get("account").isEmpty()) {
                account = input.get("account");
                if (SimpleRegionMarket.permManager.hadAdminPermissions(player)) {
                    if (input.get("account").equalsIgnoreCase("none")) {
                        account = "";
                    }
                }
            } else {
                if (SimpleRegionMarket.configurationHandler.getBoolean("Player_Line_Empty")) {
                    account = player.getName();
                } else {
                    account = SimpleRegionMarket.configurationHandler.getString("Default_Economy_Account");
                }
            }

            Utils.setEntry(this, world, region, "price", price);
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

    public void checkTakeActions(ProtectedRegion protectedRegion, String world) {

        List<String> list = Arrays.asList(SimpleRegionMarket.configurationHandler.getString("Rollback_On_Expire").split(","));
        if (list.contains(this.id)) {
            RegionSaverData data = new RegionSaverData(world, protectedRegion.getId());
            data.fromBlocks(Bukkit.getWorld(world), protectedRegion);
            SimpleRegionMarket.regionSaver.save(data);
        }
    }

    public void checkUntakeActions(ProtectedRegion protectedRegion, String world) {
        List<String> list = Arrays.asList(SimpleRegionMarket.configurationHandler.getString("Remove_LWC_On_Expire").split(","));
        if (list.contains(this.id)) {
            SimpleRegionMarket.lwcManager.removeRegionProtection(protectedRegion, world);
        }

        list = Arrays.asList(SimpleRegionMarket.configurationHandler.getString("Rollback_On_Expire").split(","));
        if (list.contains(this.id)) {
            SimpleRegionMarket.regionSaver.load(new SimpleRegionData(world, protectedRegion.getId()));
        }

        if (SimpleRegionMarket.configurationHandler.getString("Regenerate_On_Expire") != null) {
            list = Arrays.asList(SimpleRegionMarket.configurationHandler.getString("Regenerate_On_Expire").split(","));
            if (list.contains(this.id)) {
                SimpleRegionMarket.regionSaver.load(new SimpleRegionData(world, protectedRegion.getId()));
                new TerrainRegeneratorThread(Bukkit.getWorld(world), protectedRegion);
            }
        }
    }

    public void schedule(String world, String region) {
        // nothing
    }
}
