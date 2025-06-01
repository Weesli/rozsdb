package net.weesli.core.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.SneakyThrows;
import net.weesli.api.model.ObjectId;
import net.weesli.services.mapper.ObjectMapperProvider;
import net.weesli.core.model.DataMeta;
import net.weesli.core.model.ObjectIdImpl;
import net.weesli.core.util.IndexMetaUtil;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Getter
public class IndexMetaManager {
    private final ConcurrentHashMap<String, Set<DataMeta>> records = new ConcurrentHashMap<>();

    public IndexMetaManager(File file) {
        load(file);
    }

    @SneakyThrows
    public void load(File file) {
        ObjectMapper mapper = ObjectMapperProvider.getInstance();
        File[] files = file.listFiles();
        if (files != null) {
            for (File path : files) {
                if (path.isDirectory()) {
                    String name = path.getName();
                    records.put(name, ConcurrentHashMap.newKeySet());
                    File metaFile = new File(path, "meta.rozs");
                    boolean status = IndexMetaUtil.insertDefaultMeta(metaFile);
                    if (!status) continue;
                    JsonNode node = IndexMetaUtil.getMeta(metaFile);
                    if (node == null) continue;
                    JsonNode dataNode = node.get("records");
                    if (dataNode != null && dataNode.isArray()) {
                        ArrayNode arrayNode = (ArrayNode) dataNode;
                        while (arrayNode.iterator().hasNext()) {
                            JsonNode recordNode = arrayNode.get(0);
                            if (recordNode == null) continue;
                            DataMeta dataMeta = mapper.treeToValue(recordNode, DataMeta.class);
                            records.get(name).add(dataMeta);
                            arrayNode.remove(0);
                        }
                    }
                }
            }
        }
    }

    public void addRecord(String collectionName, DataMeta dataMeta) {
        Set<DataMeta> dataMetas = records.computeIfAbsent(collectionName, k -> ConcurrentHashMap.newKeySet());
        dataMetas.add(dataMeta);
    }

    public void removeRecord(String collectionName, String id) {
        Set<DataMeta> dataMetas = records.get(collectionName);
        if (dataMetas != null) {
            dataMetas.removeIf(dataMeta -> ObjectIdImpl.valueOf(dataMeta.getId()).equals(ObjectIdImpl.valueOf(id)));
        }
    }

    public Set<DataMeta> getRecords(String collectionName) {
        Set<DataMeta> dataMetas = records.get(collectionName);
        if (dataMetas != null) {
            return dataMetas;
        }
        return ConcurrentHashMap.newKeySet();
    }

    public DataMeta getRecord(String collectionName, ObjectId id) {
        Set<DataMeta> dataMetas = records.get(collectionName);
        if (dataMetas != null) {
            for (DataMeta dataMeta : dataMetas) {
                if (ObjectIdImpl.valueOf(dataMeta.getId()).equals(id)) {
                    return dataMeta;
                }
            }
        }
        return null;
    }

    @SneakyThrows
    public void saveRecords(File file) {
        for (String collectionName : records.keySet()) {
            ObjectMapper mapper = ObjectMapperProvider.getInstance();
            Set<DataMeta> dataMetas = records.get(collectionName);
            File meta = new File(new File(file, collectionName), "meta.rozs");
            ObjectNode node = mapper.createObjectNode();
            node.put("records", mapper.createArrayNode());
            JsonNode dataNode = node.get("records");
            if (dataMetas != null) {
                for (DataMeta dataMeta : dataMetas) {
                    String value = mapper.writeValueAsString(dataMeta);
                    ((ArrayNode) dataNode).add(value);
                }
            }
            // write node to file
            IndexMetaUtil.writeMeta(meta, node);
        }
    }

}
