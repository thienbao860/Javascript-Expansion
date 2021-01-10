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

            Bukkit.broadcastMessage("File name: " + fileName);
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
                   engine = new ScriptEngineManager(null).getEngineByName(config.getString(identifier + ".engine", "nashorn"));
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
        boolean debug = exp.getConfigManager().debugModeEnabled();

        final List<String> specFailed = status.pull(LogEnum.FAILED_SPEC);
        final List<String> successfulFile = status.pull(LogEnum.SUCCESSFUL_FILE);
        final List<String> failedFile = status.pull(LogEnum.FAILED_CREATE);
        final List<String> fileEmpty = status.pull(LogEnum.EMPTY_FILE);
        final List<String> emptyEngine = status.pull(LogEnum.EMPTY_ENGINE);
        final List<String> invalidEngine = status.pull(LogEnum.INVALID_ENGINE);
        final List<String> failedEngine = status.pull(LogEnum.FAILED_ENGINE);
        final List<String> loadData = status.pull(LogEnum.LOADED_DATA);
        final List<String> loadPlaceholder = status.pull(LogEnum.LOADED_PLACEHOLDER);
        final List<String> failedPlaceholder = status.pull(LogEnum.FAILED_PLACEHOLDER);

        if (specFailed.size() > 0) {
            String debugMsg = debug ? ": " + specFailed.toString() : "\n > Please enable debug in config for more info";
            ExpansionUtils.warnLog(specFailed.size() + " Javascript placeholder" + ExpansionUtils.plural(specFailed.size()) + "do not have a file specified" + debugMsg, null);
        }

        if (successfulFile.size() > 0) {
            String debugMsg = debug ? ": " + successfulFile.toString() : "\n > Please enable debug in config for more info";
            ExpansionUtils.infoLog(successfulFile.size() + " file" + ExpansionUtils.plural(successfulFile.size()) + " created!" + debugMsg + "\n" +
                    "Add your javascript to these files and use '/jsexpansion reload' to load them!");
        }

        if (failedFile.size() > 0) {
            String debugMsg = debug ? ": " + failedFile.toString() : "\n > Please enable debug in config for more info";
            ExpansionUtils.errorLog(failedFile.size() + " Javascript placeholder" + ExpansionUtils.plural(failedFile.size()) + " have a problem when creating!"
                    + debugMsg, null);
        }

        if (fileEmpty.size() > 0) {
            String debugMsg = debug ? ": " + fileEmpty.toString() : "\n > Please enable debug in config for more info";
            ExpansionUtils.warnLog(fileEmpty.size() + " Javascript placeholder" + ExpansionUtils.plural(fileEmpty.size()) + " have empty scripts."
                    + debugMsg, null);
        }

        if (emptyEngine.size() > 0) {
            String debugMsg = debug ? ": " + emptyEngine.toString() : "\n > Please enable debug in config for more info";
            ExpansionUtils.warnLog(emptyEngine.size() + " Javascript placeholder" + ExpansionUtils.plural(emptyEngine.size()) + " have not initialized its ScriptEngine!"
                    + debugMsg, null);
        }

        if (invalidEngine.size() > 0) {
            String debugMsg = debug ? ": " + invalidEngine.toString() : "\n > Please enable debug in config for more info";
            ExpansionUtils.warnLog(invalidEngine.size() + " Javascript placeholder" + ExpansionUtils.plural(invalidEngine.size()) + " have an invalid ScriptEngine and will be defaulted to global engine!"
                    + debugMsg, null);
        }

        if (failedEngine.size() > 0) {
            String debugMsg = debug ? ": " + failedEngine.toString() : "\n > Please enable debug in config for more info";
            ExpansionUtils.errorLog(failedEngine.size() + " Javascript placeholder" + ExpansionUtils.plural(failedEngine.size()) + " have failed to set ScriptEngine!"
                    + debugMsg, null);
        }

        if (loadData.size() > 0) {
            String debugMsg = debug ? ": " + loadData.toString() : "\n > Please enable debug in config for more info";
            ExpansionUtils.infoLog(loadData.size() + " Javascript placeholder" + ExpansionUtils.plural(loadData.size()) + " have loaded their Data!"
                    + debugMsg);
        }

        if (loadPlaceholder.size() > 0) {
            String debugMsg = debug ? ": " + loadPlaceholder.toString() : "\n > Please enable debug in config for more info";
            ExpansionUtils.infoLog(loadPlaceholder.size() + " Javascript placeholder" + ExpansionUtils.plural(loadPlaceholder.size()) + " have been loaded"
                    + debugMsg);
        }

        if (failedPlaceholder.size() > 0) {
            String debugMsg = debug ? ": " + failedPlaceholder.toString() : "\n > Please enable debug in config for more info";
            ExpansionUtils.warnLog(failedPlaceholder.size() + " Javascript placeholder" + ExpansionUtils.plural(failedPlaceholder.size()) + " have duplicated items"
                    + debugMsg, null);
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
