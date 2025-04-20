package net.weesli.api.database;


import net.weesli.api.CacheStore;

import java.nio.file.Path;
import java.util.List;

public interface Collection extends CacheStore {

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

    void save();
    void close();
}
