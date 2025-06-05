package net.weesli.core.model;

import lombok.SneakyThrows;
import net.weesli.api.CoreSettings;
import net.weesli.services.json.JsonBase;

import java.io.File;

public class Settings implements CoreSettings {

    private JsonBase settings;

    @SneakyThrows
    public Settings(File file){
        if (!file.exists()) return;
        settings = new JsonBase(file);
    }

    @Override
    public JsonBase getSettings() {
        return settings;
    }

    @Override
    public JsonBase get(String key) {
        return settings.getAsJson(key);
    }

}
