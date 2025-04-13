package net.weesli.core.database;

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
import net.weesli.core.index.IndexManager;
import net.weesli.core.model.DataMeta;
import net.weesli.core.timeout.TimeoutTask;
import net.weesli.core.timeout.types.CollectionTimeoutTask;
import net.weesli.core.exception.CollectionError;
import net.weesli.core.exception.CollectionTimeOutException;
import net.weesli.core.file.DatabaseFileManager;
import net.weesli.core.model.ObjectIdImpl;
import net.weesli.core.store.CacheStoreImpl;
import net.weesli.core.util.IndexMetaUtil;
import net.weesli.services.log.DatabaseLogger;
import net.weesli.services.mapper.ObjectMapperProvider;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

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
        File metaFile = new File(collectionPath.toFile(), "meta.rozs");
        IndexMetaUtil.insertDefaultMeta(metaFile);
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
        JsonNode object = getJsonObject(src);
        String jsonWithId = appendId(id, object);
        byte[] data = appendByteFormat(jsonWithId);
        cache.put(objectId, data);
        triggerAction();
        Main.core.getWritePool().enqueueWrite(database, objectId, data);
        Iterator<String> fields = object.fieldNames();
        List<String> fieldList = new ArrayList<>();
        while (fields.hasNext()) {
            fieldList.add(fields.next());
        }
        DataMeta meta = getRecords().stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
        if (meta != null){
            meta.changeFields(fieldList);
            meta.changeUpdatedAt(LocalDateTime.now().toString());
            createOrUpdateRecord(meta);
        }
        return appendByteFormat(jsonWithId);
    }

    @SneakyThrows
    @Override
    public byte[] insertOrUpdate(String src) {
        if (isTimeout()){
            throw new CollectionTimeOutException("This collection is out of time");
        }
        ObjectId id = new ObjectIdImpl();
        JsonNode object = getJsonObject(src);
        String jsonWithId = appendId(id.getObjectId(), object);
        byte[] data = appendByteFormat(jsonWithId);
        cache.put(id, data);
        triggerAction();
        Main.core.getWritePool().enqueueWrite(database, id, data);
        Iterator<String> fields = object.fieldNames();
        List<String> fieldList = new ArrayList<>();
        while (fields.hasNext()) {
            fieldList.add(fields.next());
        }
        createOrUpdateRecord(new DataMeta(id.getObjectId(), LocalDateTime.now().toString(), LocalDateTime.now().toString(), fieldList));
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
        deleteRecord(id);
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
        for (DataMeta record : getRecords()) {
            if (!record.hasField(where)) continue;

            byte[] entry = cache.get(ObjectIdImpl.valueOf(record.getId()));
            if (entry == null) continue;
            String decompressedJson = decompressJson(entry);
            if (decompressedJson == null) continue;

            try (JsonParser parser = mapper.getFactory().createParser(decompressedJson)) {
                boolean isMatch = false;
                while (parser.nextToken() != JsonToken.END_OBJECT && !isMatch) {
                    String fieldName = parser.currentName();
                    if (fieldName != null && fieldName.equals(where)) {
                        parser.nextToken(); // move to value
                        isMatch = isValueMatch(parser, value);
                    }
                }
                if (isMatch) {
                    result.add(entry);
                }
            } catch (Exception e) {
                System.err.println("Error processing entry: " + e.getMessage());
            }
        }
        triggerAction();
        return result;
    }

    private boolean isValueMatch(JsonParser parser, Object targetValue) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) {
            return targetValue == null;
        }

        if (targetValue instanceof String) {
            return parser.getValueAsString().equals(targetValue);
        } else if (targetValue instanceof Number) {
            double parsed = parser.getDoubleValue();
            double expected = ((Number) targetValue).doubleValue();
            return Math.abs(parsed - expected) < 0.000001;
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

    public JsonNode getJsonObject(String src){
        if (src == null || src.isBlank()) {
            throw new IllegalArgumentException("Input JSON string cannot be null or empty");
        }
        try {
            return mapper.readTree(src);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON input", e);
        }
    }

    public String appendId(String id, JsonNode node) throws IllegalArgumentException {
        try {
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

    private List<DataMeta> getRecords(){
        return new ArrayList<>(IndexManager.getInstance().getIndexMetaManager(database.getName()).getRecords(collectionName));
    }

    @SneakyThrows
    private void createOrUpdateRecord(DataMeta dataMeta) {
        IndexManager.getInstance().getIndexMetaManager(database.getName()).addRecord(collectionName, dataMeta);
    }

    @SneakyThrows
    private void deleteRecord(String id){
        IndexManager.getInstance().getIndexMetaManager(database.getName()).removeRecord(collectionName, id);
    }
}
