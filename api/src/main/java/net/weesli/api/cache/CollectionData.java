package net.weesli.api.cache;

import net.weesli.api.model.ObjectId;

import java.util.Map;

public interface CollectionData {
    byte[] get(ObjectId id);
    Map<ObjectId, byte[]> getDataStore();

    Map<ObjectId,byte[]> getAll();
}
