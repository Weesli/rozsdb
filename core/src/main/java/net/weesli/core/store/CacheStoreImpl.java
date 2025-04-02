package net.weesli.core.store;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.weesli.api.CacheStore;
import net.weesli.api.model.ObjectId;
import net.weesli.core.model.ObjectIdImpl;

import java.util.concurrent.ConcurrentHashMap;
@Getter
public class CacheStoreImpl implements CacheStore {
    public ConcurrentHashMap<ObjectId, String> cache;

    public CacheStoreImpl() {
        cache = new ConcurrentHashMap<>();
    }

    public JsonObject getJsonObject(String id){
        ObjectIdImpl objectId = ObjectIdImpl.valueOf(id);
        String json = cache.get(objectId);
        return JsonParser.parseString(json).getAsJsonObject();
    }

}