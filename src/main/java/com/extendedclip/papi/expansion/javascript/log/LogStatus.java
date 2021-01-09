package com.extendedclip.papi.expansion.javascript.log;

import java.util.*;

public class LogStatus {

    public Map<LogEnum, List<String>> logs;

    public LogStatus() {
        this.logs = new HashMap<>();
    }

    public void addLog(String s, LogEnum logEnum) {
        logs.putIfAbsent(logEnum, new ArrayList<>());
        logs.get(logEnum).add(s);
    }

    public List<String> pull(LogEnum logEnum) {
        return logs.get(logEnum) == null ? Collections.emptyList() : logs.get(logEnum);
    }
}
