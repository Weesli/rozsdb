package net.weesli.services.security.ip.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.weesli.services.log.DatabaseLogger;
import net.weesli.services.mapper.ObjectMapperProvider;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class IPUtil {

    public static JsonNode getCache(Path path){
        ObjectMapper mapper = ObjectMapperProvider.getInstance();
        try {
            return mapper.readTree(path.toFile());
        } catch (Exception e) {
            DatabaseLogger.logService(DatabaseLogger.LogLevel.ERROR, "Error reading cache file: " +e.getMessage());
        }
        return null;
    }

    public static List<String> getAllowedIps(JsonNode node){
        if(node == null){
            return null;
        }
        String allowed = node.get("allowed_ips").toString();
        if(allowed == null){
            return null;
        }
        allowed = allowed.replaceAll("[\\[\\]\"]", "");
        String[] allowedIps = allowed.split(",");
        if(allowedIps.length == 0){
            return Collections.emptyList();
        }
        for (int i = 0; i < allowedIps.length; i++) {
            allowedIps[i] = allowedIps[i].trim();
        }
        return List.of(allowedIps);
    }

}
