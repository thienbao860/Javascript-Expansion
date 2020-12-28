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

import java.util.HashMap;
import java.util.Map;

public class ScriptData {

    private Map<String, Object> map;

    public ScriptData(Map<String, Object> data) {
        this.map = data;
    }

    public ScriptData() {
        this.map = new HashMap<>();
    }

    public Map<String, Object> getData() {
        return map;
    }

    public void clear() {
        map.clear();
    }

    public boolean exists(String key) {
        return map.get(key) != null;
    }

    public Object get(String key) {
        return map.get(key);
    }

    public void remove(String key) {
        map.put(key, null);
    }

    public void set(String key, Object value) {
        map.put(key, ExpansionUtils.jsonToJava(value));
    }

    public void setIfNull(String key, Object value) {
        map.putIfAbsent(key, ExpansionUtils.jsonToJava(value));
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

}
