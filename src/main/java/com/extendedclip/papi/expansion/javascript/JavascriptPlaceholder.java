/*
 *
 * Javascript-Expansion
 * Copyright (C) 2020 Ryan McCarthy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.extendedclip.papi.expansion.javascript;

import com.extendedclip.papi.expansion.javascript.parser.JavascriptParser;
import com.extendedclip.papi.expansion.javascript.parser.UtilityParser;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class JavascriptPlaceholder {

    private final ScriptEngine engine;
    private final String identifier;
    private final String script;
    private ScriptData scriptData;
    private final File dataFile;
    private YamlConfiguration yaml;
    private boolean firstInit;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public JavascriptPlaceholder(final ScriptEngine engine, String identifier, String script) {
        Validate.notNull(engine, "ScriptEngine can not be null");
        Validate.notNull(identifier, "Identifier can not be null");
        Validate.notNull(script, "Script can not be null");

        String dir = PlaceholderAPIPlugin.getInstance().getDataFolder() + "/javascripts/javascript_data";
        this.firstInit = true;
        this.engine = engine;
        this.identifier = identifier;
        this.script = script;
        final File directory = new File(dir);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        scriptData = new ScriptData();
        dataFile = new File(directory, identifier + "_data.yml");
        this.engine.put("Data", scriptData);
        this.engine.put("DataVar", scriptData.getData());
        this.engine.put("BukkitServer", Bukkit.getServer());
        this.engine.put("Expansion", JavascriptExpansion.getInstance());
        this.engine.put("Placeholder", this);
        this.engine.put("PlaceholderAPI", PlaceholderAPI.class);
        this.engine.put("UtilityParser", UtilityParser.getInstance());
    }

    public String getIdentifier() {
        return identifier;
    }

    public String evaluate(OfflinePlayer player, String... args) {

        try {
            String[] arguments = null;

            if (args != null && args.length > 0) {
                arguments = new String[args.length];

                for (int i = 0; i < args.length; i++) {
                    if (args[i] == null || args[i].isEmpty()) {
                        continue;
                    }
                    arguments[i] = PlaceholderAPI.setBracketPlaceholders(player, args[i]);
                }
            }

            if (arguments == null) {
                arguments = new String[]{};
            }

            engine.put("args", arguments);

            if (player != null && player.isOnline()) {
                engine.put("BukkitPlayer", player.getPlayer());
                engine.put("Player", player.getPlayer());
            }

            engine.put("OfflinePlayer", player);
            engine.put("Parser", new JavascriptParser(player));
            Object result = engine.eval(script);
            result = ExpansionUtils.jsonToJava(result);

            if (firstInit) {
                firstInit = false;
                Invocable inv = (Invocable) engine;
                try {
                    inv.invokeFunction("firstInit");
                } catch (ScriptException | NoSuchMethodException ignored) { }
            }

            return result != null ? PlaceholderAPI.setPlaceholders(player, result.toString()) : "";

        } catch (ScriptException ex) {
            ExpansionUtils.errorLog("An error occurred while executing the script '" + identifier + "':\n\t" + ex.getMessage(), null);
        } catch (ArrayIndexOutOfBoundsException ex) {
            ExpansionUtils.errorLog("Argument out of bound while executing script '" + identifier + "':\n\t" + ex.getMessage(), null);
        }
        return "Script error (check console)";
    }

    public String getScript() {
        return script;
    }

    public ScriptData getData() {
        if (scriptData == null) {
            scriptData = new ScriptData();
        }
        return scriptData;
    }

    public void setData(ScriptData data) {
        this.scriptData = data;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean loadData() {
        yaml = new YamlConfiguration();
        dataFile.getParentFile().mkdirs();

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                ExpansionUtils.errorLog("An error occurred while creating data file for " + getIdentifier(), e);
                return false;
            }
        }

        try {
            yaml.load(dataFile);
        } catch (IOException | InvalidConfigurationException e) {
            ExpansionUtils.errorLog("An error occurred while loading for " + getIdentifier(), e);
            return false;
        }

        final Set<String> keys = yaml.getKeys(true);

        if (keys.size() == 0) {
            return false;
        }

        if (scriptData == null)
            scriptData = new ScriptData();
        else scriptData.clear();

        keys.forEach(key -> scriptData.set(key, ExpansionUtils.ymlToJavaObj(yaml.get(key))));

        if (!scriptData.isEmpty()) {
            this.setData(scriptData);
            return true;
        }
        return false;
    }

    public void saveData() {
        if (scriptData == null || scriptData.isEmpty() || yaml == null) {
            return;
        }

        scriptData.getData().forEach((key, value) -> yaml.set(key, ExpansionUtils.jsonToJava(value)));

        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            ExpansionUtils.errorLog(ExpansionUtils.PREFIX + "An error occurred while saving data for " + getIdentifier(), e);
        }
    }

    public void cleanup() {
        if (this.scriptData != null) {
            this.scriptData.clear();
            this.scriptData = null;
        }
        this.yaml = null;
    }

}
