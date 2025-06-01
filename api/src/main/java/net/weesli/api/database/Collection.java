package net.weesli.api.database;


import net.weesli.api.cache.CollectionData;
import net.weesli.api.model.ObjectId;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public interface Collection {

    Path getCollectionPath();
    String getCollectionName();
    boolean isTimeout();
    Database getDatabase();

    byte[] insertOrUpdate(String id, String src);
    byte[] insertOrUpdate(String src);
    boolean delete(String id);
    byte[] findById(String id);
    List<byte[]> find(String where, Object value);
    List<byte[]> findAll();

    CollectionData getCollectionData();

    void save();
    void close();
}
