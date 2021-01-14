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
package com.extendedclip.papi.expansion.javascript.manager;

import com.extendedclip.papi.expansion.javascript.ExpansionUtils;
import com.extendedclip.papi.expansion.javascript.JavascriptExpansion;
import com.extendedclip.papi.expansion.javascript.JavascriptPlaceholder;
import com.extendedclip.papi.expansion.javascript.log.LogEnum;
import com.extendedclip.papi.expansion.javascript.log.LogStatus;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;

public class JavascriptPlaceholdersManager {

    private final JavascriptExpansion exp;
    private final FileConfiguration config;
    private final LogStatus status;
    private final ConfigManager configManager;

    public JavascriptPlaceholdersManager(JavascriptExpansion exp) {
        this.exp = exp;
        this.configManager = exp.getConfigManager();
        this.config = configManager.getConfig();
        this.status = new LogStatus();
    }

    public int loadPlaceholders() {

        if (config == null || config.getKeys(false).isEmpty()) {
            return 0;
        }

        final File directory = new File(exp.getPlaceholderAPI().getDataFolder(), "javascripts");
        try {
            configManager.addDirectory(directory);
        } catch (IOException e) {
            ExpansionUtils.errorLog("Failed to create 'javascript' directory", e);
        }

        for (String identifier : config.getKeys(false)) {

            final String fileName = config.getString(identifier + ".file");
            if (fileName == null || !config.contains(identifier + ".file")) {
                status.addLog(identifier, LogEnum.FAILED_SPEC);
                continue;
            }

            final File scriptFile = new File(exp.getPlaceholderAPI().getDataFolder() + "/javascripts", fileName);

            if (!scriptFile.exists()) {
                ExpansionUtils.infoLog(scriptFile.getName() + " does not exist. Creating one for you...");

                try {
                    boolean canAdd = configManager.addFile(scriptFile);
                    if (canAdd) {
                        status.addLog(scriptFile.getName(), LogEnum.SUCCESSFUL_FILE);
                    }
                } catch (IOException e) {
                    status.addLog(scriptFile.getName(), LogEnum.FAILED_CREATE);
                }

                continue;
            }

            final String script = getContents(scriptFile);

            if (script == null || script.isEmpty()) {
                status.addLog(scriptFile.getName(), LogEnum.EMPTY_FILE);
                continue;
            }

            ScriptEngine engine;
            if (!config.contains(identifier + ".engine")) {
                engine = exp.getGlobalEngine();
                status.addLog(identifier, LogEnum.EMPTY_ENGINE);
            } else {
                try {
                   engine = new ScriptEngineManager(null).getEngineByName(config.getString(identifier + ".engine", ExpansionUtils.DEFAULT_ENGINE));
                } catch (NullPointerException e) {
                    status.addLog(identifier, LogEnum.INVALID_ENGINE);
                    engine = exp.getGlobalEngine();
                }
            }

            if (engine == null) {
                status.addLog(identifier, LogEnum.FAILED_ENGINE);
                continue;
            }

            Bindings bindings = engine.createBindings();
            bindings.put("polyglot.js.allowAllAccess", true);
            engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

            final JavascriptPlaceholder placeholder = new JavascriptPlaceholder(engine, identifier, script);
            final boolean added = exp.addJSPlaceholder(placeholder);

            if (added) {
                if (placeholder.loadData()) {
                    status.addLog(identifier, LogEnum.LOADED_DATA);
                }
                status.addLog(identifier, LogEnum.LOADED_PLACEHOLDER);
            } else {
                status.addLog(identifier, LogEnum.FAILED_PLACEHOLDER);
            }
        }

        finalLogPrint();
        return exp.getAmountLoaded();
    }

    private void finalLogPrint() {

        printLog(LogEnum.FAILED_SPEC, "do not have a file specified", Level.WARNING);
        printLog(LogEnum.SUCCESSFUL_FILE, "have created their files! Add your script to these files and use '/jsexpansion reload' to load them!", Level.INFO);
        printLog(LogEnum.FAILED_CREATE, "have a problem when creating their files!", Level.SEVERE);
        printLog(LogEnum.EMPTY_FILE, "have empty scripts", Level.WARNING);
        printLog(LogEnum.EMPTY_ENGINE, "have not initialized their ScriptEngine!", Level.WARNING);
        printLog(LogEnum.INVALID_ENGINE, "have an invalid ScriptEngine and will be defaulted to global engine", Level.WARNING);
        printLog(LogEnum.FAILED_ENGINE, "have failed to set ScriptEngine!", Level.SEVERE);
        printLog(LogEnum.LOADED_DATA, "have loaded their data!", Level.INFO);
        printLog(LogEnum.LOADED_PLACEHOLDER, "have loaded their placeholders!", Level.INFO);
        printLog(LogEnum.FAILED_PLACEHOLDER, "have failed to load their placeholders!", Level.SEVERE);
    }

    private void printLog(LogEnum logEnum, String message, Level level) {
        boolean debug = exp.getConfigManager().debugModeEnabled();

        List<String> finalLog = status.pull(logEnum);
        if (finalLog.size() > 0) {
            String debugMsg = debug ? ": " + finalLog.toString() : "\n > Please enable debug in config for more info";
            if (level == Level.SEVERE) {
                ExpansionUtils.errorLog(finalLog.size() + " Javascript placeholder" + ExpansionUtils.plural(finalLog.size()) + " " + message
                        + debugMsg, null);
                return;
            }

            if (level == Level.WARNING) {
                ExpansionUtils.warnLog(finalLog.size() + " Javascript placeholder" + ExpansionUtils.plural(finalLog.size()) + " " + message
                        + debugMsg, null);
                return;
            }

            if (level == Level.INFO) {
                ExpansionUtils.infoLog(finalLog.size() + " Javascript placeholder" + ExpansionUtils.plural(finalLog.size()) + " " + message
                        + debugMsg);
            }
        }

    }

    private String getContents(File file) {
        final StringBuilder sb = new StringBuilder();

        try {
            List<String> lines = Files.readAllLines(file.toPath());
            lines.forEach((line) -> sb.append(line).append("\n"));
        } catch (IOException e) {
            return null;
        }

        return sb.toString();
    }
}
