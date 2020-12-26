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

import com.extendedclip.papi.expansion.javascript.cloud.GithubScript;
import com.extendedclip.papi.expansion.javascript.command.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class JavascriptExpansionCommands extends Command {

    private final JavascriptExpansion expansion;
    private final String PERMISSION = "placeholderapi.js.admin";
    private final String command;
    private List<ICommand> subCommands;
    private CommandMap commandMap;

    public JavascriptExpansionCommands(JavascriptExpansion expansion) {
        super("jsexpansion");
        command = getName();
        this.expansion = expansion;
        this.setDescription("Javascript expansion commands");
        this.setUsage("/" + command + " <args>");
        this.setAliases(new ArrayList<>(Arrays.asList("javascriptexpansion", "jsexp")));
        this.setPermission(PERMISSION);

        try {
            final Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            commandMap = (CommandMap) field.get(Bukkit.getServer());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            ExpansionUtils.errorLog("An error occurred while accessing CommandMap.", e, true);
        }

        initCommands();
    }

    public void initCommands() {
        if (subCommands != null) {
            subCommands.clear();
        }
        subCommands = new ArrayList<>(Arrays.asList(
                new GitCommand(expansion),
                new ListCommand(expansion),
                new ParseCommand(expansion),
                new ReloadCommand(expansion),
                new DebugCommand(expansion))
        );
    }

    @Override
    public boolean execute(CommandSender sender, @NotNull String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            ExpansionUtils.sendMsg(sender, "&cYou don't have permission to do that!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        ICommand command = null;
        for (ICommand icmd : subCommands) {
            if (icmd.getAlias().equalsIgnoreCase(args[0])) {
                command = icmd;
                command.command = getName();
                break;
            }
        }

        if (command == null) {
            ExpansionUtils.sendMsg(sender, "&cInvalid expansion sub-command! Type&f /" + getName() + " &cfor help");
            return true;
        }

        command.execute(sender, sliceFirstArr(args));

        return true;
    }

    //TODO: This thing here has to be organized thoroughly later...
    @Override
    public List<String> tabComplete(CommandSender sender, @NotNull String alias, String[] args) throws IllegalArgumentException {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }

        final List<String> commands = new ArrayList<>(Arrays.asList("list", "parse", "reload"));
        final List<String> completion = new ArrayList<>();

        if (expansion.getGithubScriptManager() != null) {
            commands.add(0, "git");
        }

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], commands, completion);
        }

        if (args[0].equalsIgnoreCase("git")) {
            if (expansion.getGithubScriptManager() == null) {
                return Collections.emptyList();
            }

            if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1], Arrays.asList("download", "enable", "info", "list", "refresh"), completion);
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("download")) {
                if (expansion.getGithubScriptManager().getAvailableScripts() == null) {
                    return Collections.emptyList();
                }

                return StringUtil.copyPartialMatches(args[2], expansion.getGithubScriptManager().getAvailableScripts().stream().map(GithubScript::getName).collect(Collectors.toList()), completion);
            }
        }

        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender) {
        ExpansionUtils.sendMsg(sender,
                "&eJavascript expansion &7v: &f" + expansion.getVersion(),
                "&eCreated by: &f" + expansion.getAuthor(),
                "&eWiki: &fhttps://github.com/PlaceholderAPI/Javascript-Expansion/wiki",
                "&r",
                "&e/" + command + " reload &7- &fReload your javascripts without reloading PlaceholderAPI.",
                "&e/" + command + " list &7- &fList loaded script identifiers.",
                "&e/" + command + " parse [me/player] [code] &7- &fTest JavaScript code in chat.",
                "&e/" + command + " debug [savedata/loaddata] [identifier] &7- &fTest JavaScript code in chat."
        );

        if (expansion.getGithubScriptManager() != null) {
            ExpansionUtils.sendMsg(sender,
                    "&e/" + command + " git refresh &7- &fRefresh available Github scripts",
                    "&e/" + command + " git download [name] &7- &fDownload a script from the js expansion github.",
                    "&e/" + command + " git list &7- &fList available scripts in the js expansion github.",
                    "&e/" + command + " git info [name] &7- &fGet the description and url of a specific script."
            );
        }
    }

    protected void unregisterCommand() {
        if (commandMap != null) {

            try {
                Class<? extends CommandMap> cmdMapClass = commandMap.getClass();
                final Field f;

                //Check if the server's in 1.13+
                if (cmdMapClass.getSimpleName().equals("CraftCommandMap")) {
                    f = cmdMapClass.getSuperclass().getDeclaredField("knownCommands");
                } else {
                    f = cmdMapClass.getDeclaredField("knownCommands");
                }

                f.setAccessible(true);
                Map<String, Command> knownCmds = (Map<String, Command>) f.get(commandMap);
                knownCmds.remove(getName());
                for (String alias : getAliases()) {
                    if (knownCmds.containsKey(alias) && knownCmds.get(alias).toString().contains(getName())) {
                        knownCmds.remove(alias);
                    }
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }

            unregister(commandMap);
        }
    }

    protected void registerCommand() {
        if (commandMap == null) {
            return;
        }

        commandMap.register("papi" + getName(), this);
        this.isRegistered();
    }

    public String[] sliceFirstArr(String[] args) {
        return Arrays.stream(args).skip(1).toArray(String[]::new);
    }

}
