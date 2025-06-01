package net.weesli.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import net.weesli.api.CoreSettings;
import net.weesli.services.mapper.ObjectMapperProvider;

import java.io.File;

public class Settings implements CoreSettings {

    private JsonNode settings;

    @SneakyThrows
    public Settings(File file){
        if (!file.exists()) return;
        settings = ObjectMapperProvider.getInstance().readTree(file);
    }

    @Override
    public JsonNode getSettings() {
        return settings;
    }

    @Override
    public JsonNode get(String key) {
        return settings.get(key);
    }

}
