package net.weesli.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import net.weesli.services.mapper.ObjectMapperProvider;

import java.io.File;
import java.io.FileReader;

public class Settings {

    private JsonNode settings;

    @SneakyThrows
    public Settings(File file){
        if (!file.exists()) return;
        settings = ObjectMapperProvider.getInstance().readTree(file);
    }

    public JsonNode get(String key) {
        return settings.get(key);
    }

}
