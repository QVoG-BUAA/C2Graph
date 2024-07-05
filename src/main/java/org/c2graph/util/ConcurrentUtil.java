package org.c2graph.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentUtil {
    public static Map<String, String> concurrentMap = new ConcurrentHashMap<>();
    public static Long crossFileExternFunctionQuery(String key) {
        String value = concurrentMap.get(key);
        if (value == null) {
            return null;
        }
        return GremlinUtil.vertexKey2Id.get(value);
    }

    public static void crossFileExternFunctionStore(String key, String value) {
        concurrentMap.put(key, value);
    }
}