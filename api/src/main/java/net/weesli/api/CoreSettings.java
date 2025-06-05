package net.weesli.api;


import net.weesli.services.json.JsonBase;

public interface CoreSettings {
    JsonBase getSettings();
    JsonBase get(String key);
}
