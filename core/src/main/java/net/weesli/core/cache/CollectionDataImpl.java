package net.weesli.core.cache;

import net.weesli.api.cache.CollectionData;
import net.weesli.api.database.Collection;
import net.weesli.api.model.ObjectId;
import net.weesli.core.util.CompressUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;

public class CollectionDataImpl implements CollectionData {
    private final Collection collection;

    private Map<ObjectId, byte[]> dataStore = Collections.synchronizedMap(new LRUCache<>(1000));


    public CollectionDataImpl(Collection collection) {
        this.collection = collection;
    }

    public byte[] get(ObjectId id) {
        byte[] value = dataStore.get(id);
        if (value != null) return value;
        return getDisk(id);
    }

    private byte[] getDisk(ObjectId id) {
        Path path = new File(collection.getCollectionPath().toFile(), id.getObjectId()).toPath();
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            long fileSize = channel.size();
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            byte[] value = new byte[(int) fileSize];
            buffer.get(value);
            // push the data to the cache
            dataStore.put(id, value);
            return CompressUtil.decompress(value);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<ObjectId, byte[]> getDataStore() {
        return dataStore;
    }
}
