package net.weesli.core.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.luben.zstd.Zstd;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.weesli.api.database.Collection;
import net.weesli.api.model.ObjectId;
import net.weesli.core.file.BaseFileManager;
import net.weesli.core.model.ObjectIdImpl;
import net.weesli.core.store.CacheStoreImpl;

import java.io.File;
import java.nio.charset.StandardCharsets;
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
        load();
    }

    private void load(){
        BaseFileManager fileManager = new BaseFileManager();
        fileManager.readAllFilesInDirectory(collectionPath.toFile(), 1000).forEach((key, value) -> cache.put(ObjectIdImpl.valueOf(key), value));
    }

    @Override
    public byte[] insertOrUpdate(String id, String src) {
        ObjectId objectId = ObjectIdImpl.valueOf(id);
        String jsonWithId = appendId(id, src);
        cache.put(objectId, appendByteFormat(jsonWithId));
        return appendByteFormat(jsonWithId);
    }

    @Override
    public byte[] insertOrUpdate(String src) {
        ObjectId id = new ObjectIdImpl();
        String jsonWithId = appendId(id.getObjectId(), src);
        byte[] bytes = appendByteFormat(jsonWithId);
        cache.put(id, bytes);
        return bytes;
    }


    @SneakyThrows
    @Override
    public boolean delete(String id){
        if(!cache.containsKey(ObjectIdImpl.valueOf(id))){
            return false;
        }
        cache.remove(ObjectIdImpl.valueOf(id));
        BaseFileManager fileManager = new BaseFileManager();
        fileManager.deleteFile(new File(collectionPath.toFile(), id));
        return true;
    }

    @SneakyThrows
    @Override
    public byte[] findById(String id){
        if(!cache.containsKey(ObjectIdImpl.valueOf(id))){
            return null;
        }
        return cache.get(ObjectIdImpl.valueOf(id));
    }

    @Override
    public List<byte[]> find(String where, Object value) {
        List<byte[]> result = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        Map<ObjectId, JsonNode> decompressedCache = cache.entrySet().parallelStream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            try {
                                return mapper.readTree(Zstd.decompress(entry.getValue(), entry.getValue().length * 10));
                            } catch (Exception e) {
                                System.err.println("Error parsing JSON: " + e.getMessage());
                                return null;
                            }
                        }
                ));

        result = decompressedCache.entrySet().parallelStream()
                .filter(entry -> entry.getValue() != null && entry.getValue().has(where) &&
                        entry.getValue().get(where).asText().equals(value.toString()))
                .map(entry -> cache.get(entry.getKey()))
                .collect(Collectors.toList());
        return result;
    }




    @Override
    public List<byte[]> findAll(){
        return cache.values().stream().toList();
    }

    @SneakyThrows
    private String appendId(String id, String json){
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        ((ObjectNode) node).put("$id", id);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    private byte[] appendByteFormat(String src){
        byte[] bytes = src.getBytes(StandardCharsets.UTF_8);
        return Zstd.compress(bytes);
    }
}
