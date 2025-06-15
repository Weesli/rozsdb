package net.weesli.services.json;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.DslJson;
import com.dslplatform.json.runtime.Settings;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonBase implements Serializable {
    private Map<String, Object> data;

    private static final DslJson<Object> dslJson = new DslJson<>(Settings.withRuntime());

    public JsonBase(Map<String, Object> data) {
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public JsonBase(File file){
        try(RandomAccessFile accessFile = new RandomAccessFile(file, "r")){
            MappedByteBuffer buffer = accessFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, accessFile.length());
            byte[] bytes = new byte[(int) accessFile.length()];
            buffer.get(bytes);
            DslJson<Object> dslJson = new DslJson<>(Settings.withRuntime());
            Object o = dslJson.deserialize(Object.class, bytes, bytes.length);
            if (o instanceof Map) {
                this.data = (Map<String, Object>) o;
            }else if (o instanceof List) {
                this.data = new HashMap<>();
                this.data.put("0", o);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public JsonBase(byte[] bytes) {
        try {

            data = (Map<String, Object>) dslJson.deserialize(Object.class, bytes, bytes.length);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public JsonBase getAsJson(String key) {
        Object val = data.get(key);
        if (val instanceof Map) {
            return new JsonBase((Map<String, Object>) val);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<JsonBase> getAsJsonList(String key) {
        Object val = data.get(key);
        if (val instanceof List) {
            List<?> rawList = (List<?>) val;
            List<JsonBase> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof Map) {
                    result.add(new JsonBase((Map<String, Object>) item));
                }
            }
            return result;
        }
        return null;
    }

    public boolean isValueMatch(String key, Object targetValue) {
        Object val = data.get(key);
        if (val instanceof String) {
            return val.equals(targetValue);
        } else if (val instanceof Number) {
            return val.equals(targetValue);
        } else if (val instanceof Boolean) {
            return val.equals(targetValue);
        }else if (val instanceof List) {
            return val.equals(targetValue);
        } else if (val instanceof Map) {
            return val.equals(targetValue);
        }
        return false;
    }


    public Map<String, Object> getData() {
        return data;
    }

    public JsonObject get(String key) {
        return new JsonObject(data.get(key));
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public void remove(String key) {
        data.remove(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, Object> getAsMap(String key, Class<T> type) {
        return (Map<String, Object>) data.get(key);
    }

    // get as list
    @SuppressWarnings("unchecked")
    public <T> List<T> getAsList(String key, Class<T> type) {
        return (List<T>) data.get(key);
    }

    public String asJsonText() {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            dslJson.serialize(data, output);
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    public List<Object> getList(){
        if (data.get("0") instanceof List) {
            return (List<Object>) data.get("0");
        }
        throw new RuntimeException("Json Error: This json object is not a list.");
    }
}
