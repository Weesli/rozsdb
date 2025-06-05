package net.weesli.services.security.ip.util;

import net.weesli.services.json.JsonBase;
import net.weesli.services.log.DatabaseLogger;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class IPUtil {

    public static JsonBase getCache(Path path){
        try {
            return new JsonBase(path.toFile());
        } catch (Exception e) {
            DatabaseLogger.logService(DatabaseLogger.LogLevel.ERROR, "Error reading cache file: " +e.getMessage());
        }
        return null;
    }

    public static List<String> getAllowedIps(JsonBase node){
        if(node == null){
            return null;
        }
        List<String> allowed = node.getAsList("allowedIps", String.class);
        if(allowed == null){
            return null;
        }
        return Collections.unmodifiableList(allowed);
    }

}
