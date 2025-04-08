package net.weesli.core.database;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.luben.zstd.Zstd;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.weesli.api.database.Collection;
import net.weesli.api.database.Database;
import net.weesli.api.model.ObjectId;
import net.weesli.core.Main;
import net.weesli.core.timeout.TimeoutTask;
import net.weesli.core.timeout.types.CollectionTimeoutTask;
import net.weesli.core.exception.CollectionError;
import net.weesli.core.exception.CollectionTimeOutException;
import net.weesli.core.file.DatabaseFileManager;
import net.weesli.core.mapper.ObjectMapperProvider;
import net.weesli.core.model.ObjectIdImpl;
import net.weesli.core.store.CacheStoreImpl;
import net.weesli.services.log.DatabaseLogger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter@Setter
public class CollectionImpl extends CacheStoreImpl implements Collection {
    private ObjectMapper mapper = ObjectMapperProvider.getInstance();

    private Database database;
    private Path collectionPath;
    private String collectionName;
    private boolean timeout;
    private TimeoutTask task;

    public CollectionImpl(DatabaseImpl databaseImpl, String collectionName) {
        this.database = databaseImpl;
        this.collectionName = collectionName;
        this.collectionPath = new File(databaseImpl.getDirectory(), collectionName).toPath();
        File collectionFile = collectionPath.toFile();
        if (!collectionFile.exists()){
            collectionFile.mkdirs();
        }
        load();
        task = new CollectionTimeoutTask(this); // create a cleaner for this collection
    }

    private void load(){
        DatabaseFileManager fileManager = new DatabaseFileManager();
        fileManager.readAllFilesInDirectory(collectionPath.toFile(), 1000).forEach((key, value) -> cache.put(ObjectIdImpl.valueOf(key), value));
    }

    @SneakyThrows
    @Override
    public byte[] insertOrUpdate(String id, String src) {
        if (isTimeout()){
            throw new CollectionTimeOutException("This collection is out of time");
        }
        ObjectId objectId = ObjectIdImpl.valueOf(id);
        String jsonWithId = appendId(id, src);
        byte[] data = appendByteFormat(jsonWithId);
        cache.put(objectId, data);
        triggerAction();
        Main.core.getWritePool().enqueueWrite(database, objectId, data);
        return appendByteFormat(jsonWithId);
    }

    @SneakyThrows
    @Override
    public byte[] insertOrUpdate(String src) {
        if (isTimeout()){
            throw new CollectionTimeOutException("This collection is out of time");
        }
        ObjectId id = new ObjectIdImpl();
        String jsonWithId = appendId(id.getObjectId(), src);
        byte[] data = appendByteFormat(jsonWithId);
        cache.put(id, data);
        triggerAction();
        Main.core.getWritePool().enqueueWrite(database, id, data);
        return data;
    }


    @SneakyThrows
    @Override
    public boolean delete(String id){
        if(isTimeout()){
            throw new CollectionTimeOutException("This collection is out of time");
        }
        if(!cache.containsKey(ObjectIdImpl.valueOf(id))){
            return false;
        }
        cache.remove(ObjectIdImpl.valueOf(id));
        DatabaseFileManager fileManager = new DatabaseFileManager();
        fileManager.deleteFile(new File(collectionPath.toFile(), id)); // force delete in disk
        triggerAction();
        return true;
    }

    @SneakyThrows
    @Override
    public byte[] findById(String id){
        if(isTimeout()){
            throw new CollectionTimeOutException("This collection is out of time");
        }
        if(!cache.containsKey(ObjectIdImpl.valueOf(id))){
            return null;
        }
        triggerAction();
        return cache.get(ObjectIdImpl.valueOf(id));
    }

    @SneakyThrows
    @Override
    public List<byte[]> find(String where, Object value) {
        if (isTimeout()){
            throw new CollectionTimeOutException("This collection is out of time");
        }
        List<byte[]> result = Collections.synchronizedList(new ArrayList<>());
        JsonFactory jsonFactory = mapper.getFactory();

        cache.values().parallelStream().forEach(entry -> {
            try {
                String decompressedJson = getString(entry);
                if (decompressedJson == null) return;

                try (JsonParser parser = jsonFactory.createParser(decompressedJson)) {
                    boolean isMatch = false;
                    while (parser.nextToken() != JsonToken.END_OBJECT && !isMatch) {
                        String fieldName = parser.currentName();
                        if (fieldName != null && fieldName.equals(where)) {
                            parser.nextToken();
                            isMatch = isValueMatch(parser, value);
                        }
                    }
                    if (isMatch) {
                        result.add(entry);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing entry: " + e.getMessage());
            }
        });
        triggerAction();
        return result;
    }

    private boolean isValueMatch(JsonParser parser, Object targetValue) throws IOException {
        if (targetValue instanceof String) {
            return parser.getValueAsString().equals(targetValue);
        } else if (targetValue instanceof Number) {
            return parser.getDoubleValue() == ((Number) targetValue).doubleValue();
        } else if (targetValue instanceof Boolean) {
            return parser.getBooleanValue() == (Boolean) targetValue;
        }
        return false;
    }

    @SneakyThrows
    @Override
    public List<byte[]> findAll(){
        if(isTimeout()){
            throw new CollectionTimeOutException("This collection is out of time");
        }
        triggerAction();
        return cache.values().stream().toList();
    }

    public String appendId(String id, String json) throws IllegalArgumentException {
        if (id == null || json == null || json.isBlank()) {
            throw new IllegalArgumentException("ID and JSON must not be null or empty");
        }
        try {
            JsonNode node = mapper.readTree(json);
            if (!node.isObject()) {
                throw new IllegalArgumentException("Input JSON must be a valid JSON object");
            }

            ObjectNode objectNode = (ObjectNode) node;
            objectNode.put("$id", id);
            return mapper.writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON input", e);
        } catch (ClassCastException e) {
            throw new IllegalStateException("Failed to process JSON structure", e);
        }
    }

    private byte[] appendByteFormat(String src){
        byte[] bytes = src.getBytes(StandardCharsets.UTF_8);
        return Zstd.compress(bytes);
    }

    private void triggerAction(){
        task.reset(); // if any action detected in this collection then reset timeout in cleaner
    }

    public boolean isTimeout() {
        return timeout;
    }

    public Database getDatabase() {
        return database;
    }

    public void reload(){
        if (!cache.isEmpty()) {
            cache.clear();
        }
        DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.INFO, "Reloading collection is :" + collectionName);
        load();
    }

    @Override
    public void save() {
        DatabaseFileManager fileManager = new DatabaseFileManager();
        cache.values().forEach(entry -> {
            try {
                fileManager.writeCollection(this);
            } catch (Exception e) {
                try {
                    throw new CollectionError("Error writing collection :" + collectionName);
                } catch (CollectionError ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
}
