package me.ienze.SimpleRegionMarket;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.ienze.SimpleRegionMarket.handlers.LangHandler;
import me.ienze.SimpleRegionMarket.signs.TemplateMain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Utils {

    static final int SIGN_LINES = 4;

    public static String replaceTokens(String text, Map<String, String> replacements) {
        final Pattern pattern = Pattern.compile("\\[\\[(.+?)\\]\\]");
        final Matcher matcher = pattern.matcher(text);
        final StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            try {
                final String replacement = replacements.get(matcher.group(1));
                if (replacement != null) {
                    matcher.appendReplacement(buffer, "");
                    buffer.append(replacement);
                }
            } catch (final Exception e) {
                //TODO I remove this, because It saying error when bid end. I can't find cause of it. (repair and uncomment this)
                //Bukkit.getLogger().log(Level.INFO, "Replacement map has a misconfiguration at " + matcher.group(1));
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static HashMap<String, String> getSignInput(TemplateMain token, String[] lines) {
        final HashMap<String, String> hashMap = new HashMap<String, String>();
        for (int i = 0; i < lines.length; i++) {
            final String inputLine = Utils.getOptionString(token, "input." + (i + 1));
            if (inputLine != null) {
                final Pattern pattern = Pattern.compile("\\[\\[(.+?)\\]\\]");
                final Matcher matcher = pattern.matcher(inputLine);
                while (matcher.find()) {
                    if (lines[i].contains(":") && matcher.group(1).contains(":")) {
                        String[] m = matcher.group(1).split(":");
                        String[] l = lines[i].split(":");
                        if (m.length == l.length) {
                            for (int j = 0; j < m.length; j++) {
                                hashMap.put(m[j], l[j]);
                            }
                        }
                    } else {
                        //singe input
                        hashMap.put(matcher.group(1), lines[i]);
                    }
                }
            }
        }
        return hashMap;
    }

    public static void checkOldFilesToUpdate() {

        //if contains old hotel file rename it to rent
        File fhotel = new File(SimpleRegionMarket.getPluginDir() + "signs/hotel.yml");
        if (fhotel.exists()) {
            LangHandler.directOut(Level.INFO, "Moving hotel to rent...");
            File frent = new File(SimpleRegionMarket.getPluginDir() + "signs/rent.yml");
            frent.delete();
            fhotel.renameTo(frent);
        }

        //if contains old 1.7 file update to signs folder
        File file = new File(SimpleRegionMarket.getPluginDir() + "agents.yml");
        if (file.exists()) {

            LangHandler.directOut(Level.INFO, "old agents.yml found...");

            if (SimpleRegionMarket.configurationHandler.loadOld(file)) {
                LangHandler.directOut(Level.INFO, "Imported successfully the old agents.yml");
                file.delete();
            } else {
                LangHandler.directOut(Level.WARNING, "Found some problem when updating agents.yml to /signs/ folder");
            }
        }
    }

    public static void setEntry(TemplateMain token, String world, String region, String key, Object value) {
        if (token != null && world != null && region != null && key != null) {
            if (!token.entries.containsKey(world)) {
                token.entries.put(world, new HashMap<String, HashMap<String, Object>>());
            }
            if (!token.entries.get(world).containsKey(region)) {
                token.entries.get(world).put(region, new HashMap<String, Object>());
            }
            token.entries.get(world).get(region).put(key, value);
        }
    }

    public static void removeEntry(TemplateMain token, String world, String region, String key) {
        if (token != null && world != null && region != null && key != null) {
            if (!token.entries.containsKey(world)) {
                token.entries.put(world, new HashMap<String, HashMap<String, Object>>());
            }
            if (!token.entries.get(world).containsKey(region)) {
                token.entries.get(world).put(region, new HashMap<String, Object>());
            }
            token.entries.get(world).get(region).remove(key);
        }
    }

    public static void removeRegion(TemplateMain token, String world, String region) {
        if (token != null && world != null && region != null) {
            if (token.entries.containsKey(world)) {
                if (token.entries.get(world).containsKey(region)) {
                    token.entries.get(world).remove(region);
                }
            }
        }
    }

    public static void removeWorld(TemplateMain token, String world) {
        if (token != null && world != null) {
            if (token.entries.containsKey(world)) {
                token.entries.remove(world);
            }
        }
    }

    public static Object getEntry(TemplateMain token, String world, String region, String key) {
        if (token != null && world != null && region != null && key != null) {
            if (token.entries.containsKey(world) && token.entries.get(world).containsKey(region) && token.entries.get(world).get(region).containsKey(key)) {
                return token.entries.get(world).get(region).get(key);
            }
        }
        return null;
    }

    public static String getEntryString(TemplateMain token, String world, String region, String key) {
        Object entry = Utils.getEntry(token, world, region, key);
        if (entry != null) {
            entry = entry.toString();
        }
        return (String) entry;
    }

    public static boolean getEntryBoolean(TemplateMain token, String world, String region, String key) {
        final String strEntry = Utils.getEntryString(token, world, region, key);
        if (strEntry != null) {
            return Boolean.parseBoolean(strEntry);
        }
        return false;
    }

    public static double getEntryDouble(TemplateMain token, String world, String region, String key) {
        final String strEntry = Utils.getEntryString(token, world, region, key);
        if (strEntry != null) {
            return Double.parseDouble(strEntry);
        }
        return 0;
    }

    public static int getEntryInteger(TemplateMain token, String world, String region, String key) {
        final String strEntry = Utils.getEntryString(token, world, region, key);
        if (strEntry != null) {
            return Integer.parseInt(strEntry);
        }
        return 0;
    }

    public static long getEntryLong(TemplateMain token, String world, String region, String key) {
        final String strEntry = Utils.getEntryString(token, world, region, key);
        if (strEntry != null) {
            return Long.parseLong(strEntry);
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Location> getSignLocations(TemplateMain token, String world, String region) {
        final ArrayList<Location> signLocations = (ArrayList<Location>) Utils.getEntry(token, world, region, "signs");
        if (signLocations == null) {
            return new ArrayList<Location>();
        } else {
            return signLocations;
        }
    }

    public static void teleportToRegion(String world, String region, Player player, boolean toSign) {
        ProtectedRegion pr = SimpleRegionMarket.wgManager.getProtectedRegion(Bukkit.getWorld(world), region);

        int minX = pr.getMinimumPoint().getBlockX();
        int maxX = pr.getMaximumPoint().getBlockX();
        int minZ = pr.getMinimumPoint().getBlockZ();
        int maxZ = pr.getMaximumPoint().getBlockZ();
        Location loc = null;

        if (toSign) {
            for (TemplateMain token : TokenManager.tokenList) {
                Iterator<Location> signs = Utils.getSignLocations(token, world, region).iterator();
                if (signs.hasNext()) {
                    loc = signs.next();
                    break;
                }
            }
        } else {
            String tploc = SimpleRegionMarket.configurationHandler.getString("Teleport_Where");
            if (tploc.equalsIgnoreCase("minCorner")) {
                loc = new Location(Bukkit.getWorld(world), 0, 0, 0);
                loc.setX(minX);
                loc.setZ(minZ);
                loc.setY(loc.getWorld().getHighestBlockYAt(minX, minZ));

            }
            if (tploc.equalsIgnoreCase("centerHighest")) {
                loc = new Location(Bukkit.getWorld(world), 0, 0, 0);
                loc.setX(Math.round((maxX - minX) / 2) + minX);
                loc.setZ(Math.round((maxZ - minZ) / 2) + minZ);
                loc.setY(loc.getWorld().getHighestBlockYAt(minX, minZ));

            }
            if (tploc.equalsIgnoreCase("centerByLight")) {
                int x = Math.round((maxX - minX) / 2) + minX;
                int z = Math.round((maxZ - minZ) / 2) + minZ;
                loc = new Location(Bukkit.getWorld(world), x, 0, z);
                World w = loc.getWorld();
                for (int y = loc.getWorld().getSeaLevel(); y < loc.getWorld().getMaxHeight(); y++) {
                    if (w.getBlockAt(x, y, z).getLightFromSky() > (byte) 12) {
                        if (w.getBlockAt(x, y, z).getType() == Material.AIR) {
                            loc.setX(x);
                            loc.setY(y);
                            loc.setZ(z);
                            break;
                        }
                    }
                }
            }
        }
        if (loc != null) {
            player.teleport(loc);
        }
    }

    public static ProtectedRegion getProtectedRegion(String region, Location location, String world) {
        ProtectedRegion protectedRegion = null;
        final RegionManager worldRegionManager;
        if (world == null) {
            worldRegionManager = SimpleRegionMarket.wgManager.getWorldGuard().getRegionManager(location.getWorld());
        } else {
            worldRegionManager = SimpleRegionMarket.wgManager.getWorldGuard().getRegionManager(Bukkit.getServer().getWorld(world.toString()));
        }
        if (region == null || region.isEmpty()) {
            if (worldRegionManager.getApplicableRegions(location).size() > 1) {
                ApplicableRegionSet ApplicableRegions = worldRegionManager.getApplicableRegions(location);

                //region with highest priority
                if (SimpleRegionMarket.configurationHandler.getBoolean("Hight_Priority_Region")) {
                    int highestPrionrity = 0;
                    for (ProtectedRegion r : ApplicableRegions) {
                        int priority = r.getPriority();
                        if (priority > highestPrionrity) {
                            highestPrionrity = priority;
                            protectedRegion = r;
                        }
                    }
                } else {
                    protectedRegion = ApplicableRegions.iterator().next();
                }
            }
        } else {
            protectedRegion = worldRegionManager.getRegion(region);
        }

        return protectedRegion;
    }

    public static Object getOption(TemplateMain token, String key) {
        if (token != null && key != null) {
            if (token.tplOptions.containsKey(key)) {
                return token.tplOptions.get(key);
            }
        }
        return null;
    }

    public static String getOptionString(TemplateMain token, String key) {
        Object entry = Utils.getOption(token, key);
        if (entry != null) {
            entry = entry.toString();
        }
        return (String) entry;
    }

    public static boolean getOptionBoolean(TemplateMain token, String key) {
        final String strEntry = Utils.getOptionString(token, key);
        if (strEntry != null) {
            return Boolean.parseBoolean(strEntry);
        }
        return false;
    }

    public static double getOptionDouble(TemplateMain token, String key) {
        final String strEntry = Utils.getOptionString(token, key);
        if (strEntry != null) {
            return Double.parseDouble(strEntry);
        }
        return 0;
    }

    public static int getOptionInteger(TemplateMain token, String key) {
        final String strEntry = Utils.getOptionString(token, key);
        if (strEntry != null) {
            return Integer.parseInt(strEntry);
        }
        return 0;
    }

    public static long getOptionLong(TemplateMain token, String key) {
        final String strEntry = Utils.getOptionString(token, key);
        if (strEntry != null) {
            return Long.parseLong(strEntry);
        }
        return 0;
    }

    /**
     * Gets the sign time.
     *
     * @param time the time
     * @return the sign time
     */
    public static String getSignTime(long time) {
        time = time / 1000; // From ms to sec
        final int days = (int) (time / (24 * 60 * 60));
        time = time % (24 * 60 * 60);
        final int hours = (int) (time / (60 * 60));
        time = time % (60 * 60);
        final int minutes = (int) (time / 60);
        String strReturn = "< 1 min";
        if (days > 0) {
            strReturn = Integer.toString(days);
            if (hours > 0) {
                strReturn += "+";
            }
            if (days == 1) {
                strReturn += " day";
            } else {
                strReturn += " days";
            }
        } else if (hours > 0) {
            strReturn = Integer.toString(hours);
            if (minutes > 0) {
                strReturn += "+";
            }
            if (hours == 1) {
                strReturn += " hour";
            } else {
                strReturn += " hours";
            }
        } else if (minutes > 0) {
            strReturn = Integer.toString(minutes);
            if (minutes == 1) {
                strReturn += " min";
            } else {
                strReturn += " mins";
            }
        }
        return strReturn;
    }

    /**
     * Parses the sign time.
     *
     * @param timestring the timestring
     * @return the long
     */
    public static long parseSignTime(String timestring) {
        long time = 0;
        int i, u;

        i = timestring.indexOf("d");
        if (i > 0) {
            if (timestring.charAt(i - 1) == ' ' && i > 1) {
                i--;
            }
            u = i - 1;
            while (u > 0 && Character.isDigit(timestring.charAt(u - 1))) {
                u--;
            }
            time += Long.parseLong(timestring.substring(u, i)) * 24 * 60 * 60 * 1000;
        }

        i = timestring.indexOf("h");
        if (i > 0) {
            if (timestring.charAt(i - 1) == ' ' && i > 1) {
                i--;
            }
            u = i - 1;
            while (u > 0 && Character.isDigit(timestring.charAt(u - 1))) {
                u--;
            }
            time += Long.parseLong(timestring.substring(u, i)) * 60 * 60 * 1000;
        }

        i = timestring.indexOf("m");
        if (i > 0) {
            if (timestring.charAt(i - 1) == ' ' && i > 1) {
                i--;
            }
            u = i - 1;
            while (u > 0 && Character.isDigit(timestring.charAt(u - 1))) {
                u--;
            }
            time += Long.parseLong(timestring.substring(u, i)) * 60 * 1000;
        }

        return time;
    }

    public static int compareVersions(String v1, String v2) {
        final String s1 = normalisedVersion(v1);
        final String s2 = normalisedVersion(v2);
        final int cmp = s1.compareToIgnoreCase(s2);
        return (cmp < 0 ? 2 : cmp > 0 ? 1 : 0);
    }

    public static String normalisedVersion(String version) {
        return normalisedVersion(version, ".", 4);
    }

    public static String normalisedVersion(String version, String sep, int maxWidth) {
        final String[] split = Pattern.compile(sep, Pattern.LITERAL).split(version);
        final StringBuilder sb = new StringBuilder();
        for (final String s : split) {
            sb.append(String.format("%" + maxWidth + 's', s));
        }
        return sb.toString();
    }

    /**
     * Returns the Copyright.
     *
     * @return the copyright
     */
    public static String getCopyright() {
        return "by ienze and technisat";
        //original source by thezorro266
    }
}
