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

import com.extendedclip.papi.expansion.javascript.cloud.GithubScriptManager;
import com.extendedclip.papi.expansion.javascript.manager.ConfigManager;
import com.extendedclip.papi.expansion.javascript.manager.JavascriptPlaceholdersManager;
import com.oracle.truffle.api.Truffle;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.*;
import java.util.stream.Collectors;

public class JavascriptExpansion extends PlaceholderExpansion implements Cacheable, Configurable {

    private final ScriptEngineManager manager;
    private JavascriptPlaceholdersManager config;
    private final Set<JavascriptPlaceholder> scripts;
    private final String VERSION;
    private static JavascriptExpansion instance;
    private JavascriptExpansionCommands commands;
    private String argument_split;

    private final ConfigManager confManager;
    private GithubScriptManager githubManager;

    public JavascriptExpansion() {
        instance = this;
        this.VERSION = getClass().getPackage().getImplementationVersion();
        this.scripts = new HashSet<>();
        this.confManager = new ConfigManager(this);

//        PlaceholderAPIPlugin plugin = getPlaceholderAPI();
//        try {
//            Method m = getPlaceholderAPI().getClass().getSuperclass().getDeclaredMethod("getClassLoader");
//            m.setAccessible(true);
//            Thread.currentThread().setContextClassLoader((ClassLoader) m.invoke(plugin));
//        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
//            e.printStackTrace();
//        }
//
//        Thread.currentThread().setContextClassLoader(getPlaceholderAPI().getClass().getSuperclass().getClassLoader());
        final Class<Truffle> truffle = Truffle.class;

        this.manager = new ScriptEngineManager();
    }

    @Override
    public String getAuthor() {
        return "clip";
    }

    @Override
    public String getIdentifier() {
        return "javascript";
    }

    @Override
    public String getVersion() {
        return "1.7.0";
    }

    @Override
    public boolean register() {

        this.argument_split = getConfigManager().getSplitStr();

        if (argument_split.equals("_")) {
            argument_split = ",";
            ExpansionUtils.warnLog("Underscore character ('_') will not be allowed for splitting. Defaulting to ',' for this", null);
        }

        if (getConfigManager().debugModeEnabled()) {
            ExpansionUtils.infoLog("Java version: " + System.getProperty("java.version"));

            final List<ScriptEngineFactory> factories = manager.getEngineFactories();

            ExpansionUtils.infoLog("Displaying all script engine factories.", false);

            for (ScriptEngineFactory factory : factories) {
                System.out.println(factory.getEngineName());
                System.out.println("  Version: " + factory.getEngineVersion());
                System.out.println("  Lang name: " + factory.getLanguageName());
                System.out.println("  Lang version: " + factory.getLanguageVersion());
                System.out.println("  Extensions: ." + String.join(", .", factory.getExtensions()));
                System.out.println("  Mime types: " + String.join(", ", factory.getMimeTypes()));
                System.out.println("  Names: " + String.join(", ", factory.getNames()));
            }
        }

        this.config = new JavascriptPlaceholdersManager(this);

        int amountLoaded = config.loadPlaceholders();
        ExpansionUtils.infoLog(amountLoaded + " script" + ExpansionUtils.plural(amountLoaded) + " loaded!");

        if (getConfigManager().gitDownloadEnabled()) {
            githubManager = new GithubScriptManager(this);
            githubManager.fetch();
        }

        this.commands = new JavascriptExpansionCommands(this);
        commands.registerCommand();

        return super.register();
    }

    @Override
    public void clear() {
        commands.unregisterCommand();

        scripts.forEach(script -> {
            script.saveData();
            script.cleanup();
        });

        if (githubManager != null) {
            githubManager.clear();
            githubManager = null;
        }

        scripts.clear();
        instance = null;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null || scripts.size() == 0) {
            return "";
        }

        for (JavascriptPlaceholder script : scripts) {
            if (identifier.startsWith(script.getIdentifier() + "_")) {
                identifier = identifier.replaceFirst(script.getIdentifier() + "_", "");

                return !identifier.contains(argument_split) ?
                        script.evaluate(player, identifier) :
                        script.evaluate(player, identifier.split(argument_split));
            }

            if (identifier.equalsIgnoreCase(script.getIdentifier())) {
                return script.evaluate(player);
            }
        }

        return null;
    }

    public boolean addJSPlaceholder(JavascriptPlaceholder placeholder) {
        if (placeholder == null) {
            return false;
        }

        if (scripts.isEmpty()) {
            scripts.add(placeholder);
            return true;
        }

        if (getJSPlaceholder(placeholder.getIdentifier()) != null) {
            return false;
        }

        scripts.add(placeholder);
        return true;
    }

//    public Set<JavascriptPlaceholder> getJSPlaceholders() {
//        return scripts;
//    }

    public List<String> getLoadedIdentifiers() {
        return scripts.stream()
                .map(JavascriptPlaceholder::getIdentifier)
                .collect(Collectors.toList());
    }

    public JavascriptPlaceholder getJSPlaceholder(String identifier) {
        return scripts.stream()
                .filter(s -> s.getIdentifier().equalsIgnoreCase(identifier))
                .findFirst()
                .orElse(null);
    }

    public int getAmountLoaded() {
        return scripts.size();
    }

    public JavascriptPlaceholdersManager getConfig() {
        return config;
    }

    @Override
    public Map<String, Object> getDefaults() {
        final Map<String, Object> defaults = new HashMap<>();
        defaults.put("debug", false);
        defaults.put("argument_split", ",");
        defaults.put("github_script_downloads", false);

        return defaults;
    }

    public int reloadScripts() {
        scripts.forEach(script -> {
            script.saveData();
            script.cleanup();
        });

        scripts.clear();
        getConfigManager().reload();
        return config.loadPlaceholders();
    }

    public static JavascriptExpansion getInstance() {
        return instance;
    }

    public GithubScriptManager getGithubScriptManager() {
        return githubManager;
    }

    public void setGithubScriptManager(GithubScriptManager manager) {
        this.githubManager = manager;
    }


    public ConfigManager getConfigManager() {
        return confManager;
    }
}
