package net.weesli.api;

import com.fasterxml.jackson.databind.JsonNode;

public interface CoreSettings {
    JsonNode getSettings();
    JsonNode get(String key);
}
