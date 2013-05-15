package me.ienze.SimpleRegionMarket.handlers;

/*
 *
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import me.ienze.SimpleRegionMarket.SimpleRegionMarket;
import me.ienze.SimpleRegionMarket.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class LangHandler {

    private static final FileConfiguration languageFile = new YamlConfiguration();
    private static SimpleRegionMarket plugin;

    public LangHandler(SimpleRegionMarket plugin, String lang) {
        LangHandler.plugin = plugin;

        try {
            languageFile.load(SimpleRegionMarket.getPluginDir() + "lang/" + lang + ".yml");
        } catch (final FileNotFoundException e) {
            plugin.saveResource("lang/" + lang + ".yml", false);
            return;
        } catch (final IOException e) {
            e.printStackTrace();
            return;
        } catch (final InvalidConfigurationException e) {
            e.printStackTrace();
            return;
        }
        final String languageVersion = languageFile.getString("version");
        try {
            languageFile.load(plugin.getResource("lang/" + lang + ".yml"));
        } catch (final IOException e) {
            e.printStackTrace();
            return;
        } catch (final InvalidConfigurationException e) {
            e.printStackTrace();
            return;
        }
        if (languageVersion == null || Utils.compareVersions(languageFile.getString("version"), languageVersion) == 1) {
            plugin.saveResource("lang/" + lang + ".yml", true);
        }
    }

    public static void directOut(Level level, String string) {
        if (SimpleRegionMarket.configurationHandler.getBoolean("Logging") || level != Level.FINEST) {
            Bukkit.getLogger().log(level, "[SimpleRegionMarket] " + string);
        }
    }

    public static void Out(Player p, ChatColor color, String string) {
        p.sendMessage(ChatColor.WHITE + "[" + ChatColor.DARK_BLUE + "SRM" + ChatColor.WHITE + "] " + color + parseLanguageString(string, null));
    }

    public static void Out(Player p, ChatColor color, String[] string) {
        for (String s : string) {
            p.sendMessage(ChatColor.WHITE + "[" + ChatColor.DARK_BLUE + "SRM" + ChatColor.WHITE + "] " + color + parseLanguageString(s, null));
        }
    }

    public static void Out(CommandSender p, ChatColor color, String string) {
        p.sendMessage(ChatColor.WHITE + "[" + ChatColor.DARK_BLUE + "SRM" + ChatColor.WHITE + "] " + color + parseLanguageString(string, null));
    }

    public static void Out(CommandSender p, ChatColor color, String[] string) {
        for (String s : string) {
            p.sendMessage(ChatColor.WHITE + "[" + ChatColor.DARK_BLUE + "SRM" + ChatColor.WHITE + "] " + color + parseLanguageString(s, null));
        }
    }

    public static void consoleOut(String id, Level level, ArrayList<String> args) {
        directOut(level, parseLanguageString(id, args));
    }

    public static void ListOut(Player p, String id, ArrayList<String> args) {
        Out(p, ChatColor.GREEN, parseLanguageString(id, args));
    }

    public static void ErrorOut(Player p, String id, ArrayList<String> args) {
        Out(p, ChatColor.RED, parseLanguageString(id, args));
    }

    public static void NormalOut(Player p, String id, ArrayList<String> args) {
        Out(p, ChatColor.GREEN, parseLanguageString(id, args));
    }

    /**
     * Parses the language string.
     *
     * @param id the id
     * @param args the args
     * @return the string
     */
    private static String parseLanguageString(String id, ArrayList<String> args) {
        String string = id;

        String lang = SimpleRegionMarket.configurationHandler.getString("Language");
        if (!new File(SimpleRegionMarket.getPluginDir() + "lang/" + lang + ".yml").exists()) {
            directOut(Level.WARNING, "Language '" + lang + "' was not found.");
            lang = "en";
            SimpleRegionMarket.configurationHandler.getConfig().set("Language", lang);
            plugin.saveConfig();
        }

        try {
            languageFile.load(SimpleRegionMarket.getPluginDir() + "lang/" + lang + ".yml");
            string = languageFile.getString(id);
        } catch (final FileNotFoundException e1) {
            directOut(Level.SEVERE, "No write permissions on '" + SimpleRegionMarket.getPluginDir() + "'.");
            e1.printStackTrace();
        } catch (final IOException e1) {
            directOut(Level.SEVERE, "IO Exception in language system.");
            e1.printStackTrace();
        } catch (final InvalidConfigurationException e1) {
            directOut(Level.SEVERE, "Language file corrupt (Invalid YAML).");
            e1.printStackTrace();
        } catch (final Exception e1) {
            e1.printStackTrace();
        }

        if (string == null || string.isEmpty()) {
            string = id;
        }

        for (int i = string.length() - 1; i > -1; i--) {
            if (string.charAt(i) == '$') {
                if (i != 0 && string.charAt(i - 1) == '$') {
                    string = string.substring(0, i) + string.substring(i + 1, string.length());
                } else if (Character.isDigit(string.charAt(i + 1))) {
                    int argi;
                    try {
                        argi = Integer.parseInt(Character.toString(string.charAt(i + 1)));
                    } catch (final Exception e) {
                        string = string.substring(0, i) + "ERROR ARGUMENT" + string.substring(i + 2, string.length());
                        continue;
                    }

                    try {
                        string = string.substring(0, i) + args.get(argi) + string.substring(i + 2, string.length());
                    } catch (final Exception e) {
                        string = string.substring(0, i) + "ERROR ARGUMENT" + string.substring(i + 2, string.length());
                        continue;
                    }
                }
            }
        }
        return string;
    }
}
