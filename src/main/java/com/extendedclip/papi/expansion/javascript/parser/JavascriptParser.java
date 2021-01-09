package com.extendedclip.papi.expansion.javascript.parser;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class JavascriptParser {

    private final OfflinePlayer player;

    public JavascriptParser(OfflinePlayer player) {
        this.player = player;
    }

    public String parse(String msg) {
        return PlaceholderAPI.setPlaceholders(player, msg);
    }

    public String parseBracket(String msg) {
        return PlaceholderAPI.setBracketPlaceholders(player, msg);
    }

    public String parseRelational(String msg, Player relation) {
        return PlaceholderAPI.setRelationalPlaceholders(player.getPlayer(), relation, msg);
    }
}
