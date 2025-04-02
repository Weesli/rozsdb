package net.weesli.core.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileReader;

public class Settings {

    private JsonObject settings;

    @SneakyThrows
    public Settings(File file){
        if (!file.exists()) return;
        JsonReader reader = new JsonReader(new FileReader(file));
        settings = JsonParser.parseReader(reader).getAsJsonObject();
    }

    public JsonElement get(String key) {
        return settings.get(key);
    }

}
