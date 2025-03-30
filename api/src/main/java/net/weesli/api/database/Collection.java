package net.weesli.api.database;


import net.weesli.api.CacheStore;

import java.nio.file.Path;
import java.util.List;

public interface Collection extends CacheStore {

    Path getCollectionPath();
    String getCollectionName();

    void insertOrUpdate(String id, String src);
    void insertOrUpdate(String src);
    void delete(String id);
    String findById(String id);
    List<String> find(String where, Object value);
    List<String> findAll();
}
