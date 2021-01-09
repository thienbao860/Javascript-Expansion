package com.extendedclip.papi.expansion.javascript.manager;

import com.extendedclip.papi.expansion.javascript.ExpansionUtils;
import com.extendedclip.papi.expansion.javascript.JavascriptExpansion;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final JavascriptExpansion exp;
    private FileConfiguration config;
    private File file;
    private final PlaceholderAPIPlugin plugin;

    public ConfigManager(JavascriptExpansion exp) {
        this.exp = exp;
        plugin = exp.getPlaceholderAPI();
        reload();
    }

    public void reload() {
        if (file == null) {
            file = new File(plugin.getDataFolder(), "javascript_placeholders.yml");
        }

        config = YamlConfiguration.loadConfiguration(file);
        config.options().header("Javascript Expansion: " + exp.getVersion()
                + "\nThis is the main configuration file for the Javascript Expansion."
                + "\n"
                + "\nYou will define your javascript placeholders in this file."
                + "\n"
                + "\nJavascript files must be located in the:"
                + "\n /plugins/placeholderapi/javascripts/ folder"
                + "\n"
                + "\nA detailed guide on how to create your own javascript placeholders"
                + "\ncan be found here:"
                + "\nhttps://github.com/PlaceholderAPI-Expansions/Javascript-Expansion/wiki"
                + "\n"
                + "\nYour javascript placeholders will be identified by: %javascript_<identifier>%"
                + "\n"
                + "\nConfiguration format:"
                + "\n"
                + "\n<identifier>:"
                + "\n  file: <name of file>.<file extension>"
                + "\n  engine: (name of script engine)"
                + "\n"
                + "\n"
                + "\nExample:"
                + "\n"
                + "\n'my_placeholder':"
                + "\n  file: 'my_placeholder.js'"
                + "\n  engine: 'nashorn'");

        if (config.getKeys(false).isEmpty()) {
            config.set("example.file", "example.js");
            config.set("example.engine", ExpansionUtils.DEFAULT_ENGINE);
        }

        saveData();
    }

    public FileConfiguration loadData() {
        if (config == null) reload();
        return config;
    }

    public void saveData() {
        if (config == null || file == null) {
            return;
        }

        try {
            loadData().save(file);
        } catch (IOException ex) {
            ExpansionUtils.warnLog("Could not save to " + file, ex);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getSplitStr() {
        return exp.getString("argument_split", ",");
    }

    public boolean debugModeEnabled() {
        return (boolean) exp.get("debug", false);
    }

    public boolean gitDownloadEnabled() {
        return (boolean) exp.get("github_script_downloads", false);
    }
}
