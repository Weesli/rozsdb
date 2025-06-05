package net.weesli.core.database;

import com.github.luben.zstd.Zstd;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.weesli.api.cache.CollectionData;
import net.weesli.api.database.Collection;
import net.weesli.api.database.Database;
import net.weesli.api.model.ObjectId;
import net.weesli.core.Main;
import net.weesli.core.cache.CollectionDataImpl;
import net.weesli.core.index.IndexManager;
import net.weesli.core.model.DataMeta;
import net.weesli.core.timeout.TimeoutTask;
import net.weesli.core.timeout.types.CollectionTimeoutTask;
import net.weesli.core.exception.CollectionError;
import net.weesli.core.exception.CollectionTimeOutException;
import net.weesli.core.file.DatabaseFileManager;
import net.weesli.core.model.ObjectIdImpl;
import net.weesli.core.util.CompressUtil;
import net.weesli.core.util.IndexMetaUtil;
import net.weesli.services.json.JsonBase;
import net.weesli.services.log.DatabaseLogger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

@Getter@Setter
public class CollectionImpl implements Collection {

    private Database database;
    private Path collectionPath;
    private String collectionName;
    private boolean timeout;
    private TimeoutTask task;

    private CollectionData collectionData;
    private Map<ObjectId, byte[]> dataStore;

    public CollectionImpl(DatabaseImpl databaseImpl, String collectionName) {
        this.collectionData = new CollectionDataImpl(this);
        this.dataStore = collectionData.getDataStore();
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
        fileManager.readAllFilesInDirectory(collectionPath.toFile(), 1000).forEach((key, value) -> dataStore.put(ObjectIdImpl.valueOf(key), value));
    }

    @SneakyThrows
    @Override
    public byte[] insertOrUpdate(String id, String src) {
        if (isTimeout()){
            throw new CollectionTimeOutException("This collection is out of time");
        }
        ObjectId objectId = ObjectIdImpl.valueOf(id);
        JsonBase object = getJsonObject(src);
        String jsonWithId = appendId(id, object);
        byte[] data = appendByteFormat(jsonWithId);
        dataStore.put(objectId, data);
        triggerAction();
        Main.core.getWritePool().enqueueWrite(database, objectId, data);
        Iterator<String> fields = object.getData().keySet().iterator();
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
        JsonBase object = getJsonObject(src);
        String jsonWithId = appendId(id.getObjectId(), object);
        byte[] data = appendByteFormat(jsonWithId);
        dataStore.put(id, data);
        triggerAction();
        Main.core.getWritePool().enqueueWrite(database, id, data);
        Iterator<String> fields = object.getData().keySet().iterator();
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
        if(!dataStore.containsKey(ObjectIdImpl.valueOf(id))){
            return false;
        }
        dataStore.remove(ObjectIdImpl.valueOf(id));
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
        if(!dataStore.containsKey(ObjectIdImpl.valueOf(id))){
            return null;
        }
        triggerAction();
        return dataStore.get(ObjectIdImpl.valueOf(id));
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
            byte[] entry = dataStore.get(ObjectIdImpl.valueOf(record.getId()));
            if (entry == null) continue;
            byte[] decompressedBytes = CompressUtil.decompress(entry);
            JsonBase base = new JsonBase(decompressedBytes);
            if (base.has(where) && base.isValueMatch(where, value)) {
                result.add(entry);
            }
        }
        triggerAction();
        return result;
    }

    @SneakyThrows
    @Override
    public List<byte[]> findAll(){
        if(isTimeout()){
            throw new CollectionTimeOutException("This collection is out of time");
        }
        triggerAction();
        return dataStore.values().stream().toList();
    }

    public JsonBase getJsonObject(String src){
        if (src == null || src.isBlank()) {
            throw new IllegalArgumentException("Input JSON string cannot be null or empty");
        }
        return new JsonBase(src.getBytes(StandardCharsets.UTF_8));
    }

    public String appendId(String id, JsonBase node) throws IllegalArgumentException {
        node.put("$id", id);
        return node.asJsonText();
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
        if (!dataStore.isEmpty()) {
            dataStore.clear();
        }
        DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.INFO, "Reloading collection is :" + collectionName);
        load();
    }

    @Override
    public void save() {
        DatabaseFileManager fileManager = new DatabaseFileManager();
        dataStore.values().forEach(entry -> {
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

    @Override
    public void close() {
        task.cancel();
        save();
        dataStore.clear();
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
