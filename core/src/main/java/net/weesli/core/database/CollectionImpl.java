package net.weesli.core.database;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.weesli.api.database.Collection;
import net.weesli.api.model.ObjectId;
import net.weesli.core.file.BaseFileManager;
import net.weesli.core.model.ObjectIdImpl;
import net.weesli.core.store.CacheStoreImpl;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    public String insertOrUpdate(String id, String src){
        cache.put(ObjectIdImpl.valueOf(id), src);
        return appendId(id, src);
    }

    @Override
    public String insertOrUpdate(String src) {
        ObjectId id = new ObjectIdImpl();
        cache.put(id, src);
        return appendId(id.getObjectId(), src);
    }

    @SneakyThrows
    @Override
    public boolean delete(String id){
        if(!cache.containsKey(ObjectIdImpl.valueOf(id))){
            return false;
        }
        cache.remove(ObjectIdImpl.valueOf(id));
        BaseFileManager fileManager = new BaseFileManager();
        fileManager.deleteFile(new File(collectionPath.toFile(), id + ".json"));
        return true;
    }

    @SneakyThrows
    @Override
    public String findById(String id){
        if(!cache.containsKey(ObjectIdImpl.valueOf(id))){
            return null;
        }
        String json = cache.getOrDefault(ObjectIdImpl.valueOf(id), null);
        return json != null? appendId(id, json) : null;
    }

    @Override
    public List<String> find(String where, Object value) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<ObjectId, String> entry : cache.entrySet()){
            JsonObject element = JsonParser.parseString(entry.getValue()).getAsJsonObject();
            if (element.has(where) && element.get(where).equals(value)) {
                result.add(appendId(entry.getKey().getObjectId(), entry.getValue()));
            }
        }
        return result;
    }

    @Override
    public List<String> findAll(){
        return cache.entrySet().stream().filter(entry -> {
            try {
                JsonParser.parseString(entry.getValue()).getAsJsonObject();
                return true;
            }catch (Exception e){
                return false;
            }
        }).map(entry -> appendId(entry.getKey().getObjectId(), entry.getValue())).collect(Collectors.toList());
    }

    private String appendId(String id, String json){
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        jsonObject.addProperty("$id", id);
        return jsonObject.toString();
    }
}
