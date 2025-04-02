package net.weesli.core.database;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import net.weesli.api.database.Collection;
import net.weesli.core.file.BaseFileManager;
import net.weesli.core.model.ObjectIdImpl;
import net.weesli.core.store.CacheStoreImpl;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter@Setter
public class CollectionImpl extends CacheStoreImpl implements Collection {

    private Path collectionPath;
    private String collectionName;

    public CollectionImpl(DatabaseImpl databaseImpl, String collectionName) {
        this.collectionName = collectionName;
        this.collectionPath = new File(databaseImpl.getDirectory(), collectionName).toPath();
        File collectionFile = collectionPath.toFile();
        if (!collectionFile.exists()){
            collectionFile.mkdirs();
        }
        // load all data from the collection file
        load();
    }

    private void load(){
        BaseFileManager fileManager = new BaseFileManager();
        fileManager.readAllFilesInDirectory(collectionPath.toFile(), 1000).forEach((key, value) -> cache.put(ObjectIdImpl.valueOf(key), value));
    }

    @Override
    public void insertOrUpdate(String id, String src){
        cache.put(ObjectIdImpl.valueOf(id), src);
    }

    @Override
    public void insertOrUpdate(String src) {
        cache.put(new ObjectIdImpl(), src);
    }

    @Override
    public void delete(String  id){
        cache.remove(ObjectIdImpl.valueOf(id));
    }

    @Override
    public String findById(String id){
        return cache.getOrDefault(ObjectIdImpl.valueOf(id), null);
    }

    @Override
    public List<String> find(String where, Object value) {
        Set<JsonObject> elements = cache
                .values().stream().map(line -> JsonParser.parseString(line).getAsJsonObject()).collect(Collectors.toSet());
        List<String> result = new ArrayList<>();
        for (JsonObject element : elements) {
            if (element.has(where) && element.get(where).getAsString().equals(value.toString())) {
                result.add(element.toString());
            }
        }
        return result;
    }

    @Override
    public List<String> findAll(){

        return cache.values().stream().filter(string -> {
            try {
                JsonParser.parseString(string).getAsJsonObject();
                return true;
            }catch (Exception e){
                return false;
            }
        }).map(string -> JsonParser.parseString(string).getAsJsonObject()).map(JsonObject::toString).collect(Collectors.toList());
    }
}
