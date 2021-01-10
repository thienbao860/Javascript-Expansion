package com.extendedclip.papi.expansion.javascript.parser;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UtilityParser {

    private static UtilityParser instance;

    public static UtilityParser getInstance() {
        if (instance == null) {
            instance = new UtilityParser();
        }
        return instance;
    }

    public String readJSON(String url) throws IOException {

        URL u = new URL(url);
        HttpURLConnection httpCon = (HttpURLConnection) u.openConnection();
        httpCon.addRequestProperty("User-Agent", "Chrome");
        httpCon.connect();

        StringBuilder sb = new StringBuilder();
        try (InputStream is = httpCon.getInputStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }

        }
        String result = sb.toString();
//        if ((result.startsWith("{") && result.endsWith("}")) || (result.startsWith("[") && result.endsWith("]"))) {
//            return sb.toString();
//        }

        if (isValidJSON(result)) {
            return result;
        }
        return "";
    }

    private boolean isValidJSON(String json) {
        try {
            new Gson().fromJson(json, Object.class);
            return true;
        } catch(com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }
}
