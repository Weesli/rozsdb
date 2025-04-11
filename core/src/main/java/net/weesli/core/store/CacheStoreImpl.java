package net.weesli.core.store;

import com.github.luben.zstd.Zstd;
import lombok.Getter;
import net.weesli.api.CacheStore;
import net.weesli.api.model.ObjectId;
import net.weesli.core.model.ObjectIdImpl;

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
        try {
            long decompressedSize = Zstd.decompressedSize(value);
            if (decompressedSize <= 0) {
                decompressedSize = value.length * 10L;
            }

            byte[] decompressed = Zstd.decompress(value, (int)decompressedSize);
            return new String(decompressed, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Decompression failed: " + e.getMessage());
            return null;
        }
    }


}