package com.extendedclip.papi.expansion.javascript;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemorySection;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ExpansionUtils {

    public static final String PREFIX = "[PAPI] [Javascript-Expansion] ";
    private static final Logger logger = Bukkit.getLogger();

    public static @NotNull String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void sendMsg(CommandSender sender, String... msg) {
        sender.sendMessage(colorize(Arrays.stream(msg).filter(Objects::nonNull).collect(Collectors.joining("\n"))));
    }

    public static String plural(final int amount) {
        return amount > 1 ? "s" : "";
    }

    public static void warnLog(String log, Throwable throwable) {
        warnLog(log, throwable, true);
    }

    public static void infoLog(final String log) {
        infoLog(log, true);
    }

    public static void infoLog(String log, boolean canPrefix) {
        String prefix = "";
        if (canPrefix) prefix = PREFIX;
        logger.info(colorize(prefix + log));
    }

    public static void warnLog(String log, Throwable throwable, boolean canPrefix) {
        String prefix = "";
        if (canPrefix) prefix = PREFIX;
        if (throwable == null) {
            logger.log(Level.WARNING, prefix + log);
        } else logger.log(Level.WARNING, prefix + log, throwable);
    }

    public static void errorLog(String log, Throwable throwable) {
        errorLog(log, throwable, true);
    }

    public static void errorLog(String log, Throwable throwable, boolean canPrefix) {
        String prefix = "";
        if (canPrefix) prefix = PREFIX;
        if (throwable == null) {
            logger.log(Level.SEVERE, prefix + log);
        } else {
            logger.log(Level.SEVERE, prefix + log, throwable);
        }
    }

    protected static Object jsonToJava(Object jsObj) {
//        Value value = Value.asValue(jsObj);

        if (jsObj instanceof Map) {
            return jsObj;
//            if (value.hasArrayElements()) {
//                List<Object> list = new ArrayList<>();
//                for (int i = 0; i < value.getArraySize(); i++) {
//                    list.add(jsonToJava(value.getArrayElement(i)));
//                }
////                for (Map.Entry<String, Object> entry : value.getMemberKeys()) {
////                    list.add(jsonToJava(entry.getValue()));
////                }
//                return list;
//            } else {
////                Map<String, Object> map = new HashMap<>();
////                for (Map.Entry<String, Object> entry : value.entrySet()) {
////                    map.put(entry.getKey(), jsonToJava(entry.getValue()));
////                }
//                Map<String, Object> map = new HashMap<>();
//                for (String key : value.getMemberKeys()) {
//                    map.put(key, jsonToJava(value.getMember(key)));
//                }
//                return map;
//            }

        } else {
            return jsObj;
        }
    }

    protected static Object ymlToJavaObj(Object obj) {
        if (obj instanceof MemorySection) {
            MemorySection ymlMem = (MemorySection) obj;
            if (ymlMem.isList(ymlMem.getCurrentPath())) {
                ArrayList<Object> list = new ArrayList<>();
                for (String entry : ymlMem.getKeys(true)) {
                    list.add(ymlToJavaObj(ymlMem.get(entry)));
                }
                return list;
            } else {
                Map<String, Object> map = new HashMap<>();
                for (String entry : ymlMem.getKeys(true)) {
                    map.put(entry, ymlToJavaObj(ymlMem.get(entry)));
                }
                return map;
            }
        } else {
            return obj;
        }
    }

}
