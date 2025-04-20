package net.weesli.core.store;

import lombok.Getter;
import net.weesli.api.CacheStore;
import net.weesli.api.model.ObjectId;
import net.weesli.core.model.ObjectIdImpl;
import net.weesli.core.util.CompressUtil;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
@Getter
public class CacheStoreImpl implements CacheStore {
    public ConcurrentHashMap<ObjectId, byte[]> cache;

    public CacheStoreImpl() {
        cache = new ConcurrentHashMap<>();
    }

    public String decompressJson(String id){
        ObjectIdImpl objectId = ObjectIdImpl.valueOf(id);
        byte[] value = cache.get(objectId);
        return value == null? null : decompressJson(value);
    }

    public String decompressJson(byte[] value) {
        if (value == null) {
            return null;
        }
        byte[] decompressed = CompressUtil
                .decompress(value);
        return new String(decompressed, StandardCharsets.UTF_8);
    }


}