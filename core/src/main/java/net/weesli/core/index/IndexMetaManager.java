package net.weesli.core.index;

import com.dslplatform.json.DslJson;
import lombok.Getter;
import lombok.SneakyThrows;
import net.weesli.api.model.ObjectId;
import net.weesli.core.JsonUtil;
import net.weesli.core.model.DataMeta;
import net.weesli.core.model.ObjectIdImpl;
import net.weesli.core.util.IndexMetaUtil;
import net.weesli.services.json.JsonBase;
import org.apache.commons.text.StringEscapeUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.weesli.core.JsonUtil.dslJson;


@Getter
public class IndexMetaManager {
    private final ConcurrentHashMap<String, Set<DataMeta>> records = new ConcurrentHashMap<>();

    public IndexMetaManager(File file) {
        load(file);
    }

    @SneakyThrows
    public void load(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File path : files) {
                if (path.isDirectory()) {
                    String name = path.getName();
                    records.put(name, ConcurrentHashMap.newKeySet());
                    File metaFile = new File(path, "meta.rozs");
                    boolean status = IndexMetaUtil.insertDefaultMeta(metaFile);
                    if (!status) continue;
                    JsonBase node = IndexMetaUtil.getMeta(metaFile);
                    if (node == null) continue;
                    List<String> records = node.getAsList("records", String.class);
                    if (records != null) {
                        for (String record : records) {
                            String escaped = record;
                            if (escaped.startsWith("\"") && escaped.endsWith("\"")) {
                                escaped = org.apache.commons.text.StringEscapeUtils.unescapeJson(
                                        escaped.substring(1, escaped.length() - 1)
                                );
                            }
                            byte[] bytes = escaped.getBytes(StandardCharsets.UTF_8);
                            DataMeta dataMeta = dslJson.deserialize(DataMeta.class, bytes, bytes.length);
                            addRecord(name, dataMeta);
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
            Set<DataMeta> dataMetas = records.get(collectionName);
            File meta = new File(new File(file, collectionName), "meta.rozs");
            JsonBase node = new JsonBase(new HashMap<>());
            List<String> values = new ArrayList<>();
            if (dataMetas != null) {
                for (DataMeta dataMeta : dataMetas) {
                    DslJson<Object> dslJson = JsonUtil.dslJson;
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    dslJson.serialize(dataMeta, output);
                    values.add(output.toString(StandardCharsets.UTF_8));
                }
            }
            node.put("records", values);
            // write node to file
            IndexMetaUtil.writeMeta(meta, node);
        }
    }

}
