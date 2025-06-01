package net.weesli.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.luben.zstd.Zstd;
import net.weesli.services.mapper.ObjectMapperProvider;
import net.weesli.services.log.DatabaseLogger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class IndexMetaUtil {

    public static boolean insertDefaultMeta(File file){
        if (!file.exists()) {
            JsonNode meta = getBaseMeta();
            writeMeta(file, meta);
        }
        return true;
    }

    private static JsonNode getBaseMeta(){
        ObjectMapper mapper = ObjectMapperProvider.getInstance();
        ObjectNode node = mapper.createObjectNode();
        node.put("records", mapper.createArrayNode());
        return node;
    }

    public static JsonNode getMeta(File file) {
        try {
            if (!file.exists() || file.length() == 0) {
                return null;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] decompressed = CompressUtil.decompress(bytes);

            ObjectMapper mapper = ObjectMapperProvider.getInstance();
            JsonNode root = mapper.readTree(new String(decompressed, StandardCharsets.UTF_8));

            JsonNode recordsNode = root.get("records");
            if (recordsNode != null && recordsNode.isArray()) {
                ArrayNode correctedRecords = mapper.createArrayNode();
                for (JsonNode strNode : recordsNode) {
                    if (strNode.isTextual()) {
                        JsonNode objNode = mapper.readTree(strNode.asText());
                        correctedRecords.add(objNode);
                    }
                }
                ((ObjectNode) root).set("records", correctedRecords);
            }
            return root;

        } catch (IOException e) {
            DatabaseLogger.logCore(DatabaseLogger.LogLevel.ERROR, "Failed to read meta file: " + file.getName() + " - " + e.getMessage());
        }
        return null;
    }





    public static void writeMeta(File file, JsonNode node) {
        try {
            ObjectMapper mapper = ObjectMapperProvider.getInstance();
            byte[] compressed = CompressUtil.compress(node.toString().getBytes(StandardCharsets.UTF_8));
            try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(compressed);
                while (buffer.hasRemaining()) {
                    int result = channel.write(buffer);
                    if (result == -1) {
                        throw new IOException("Failed to write meta file: " + file.getName());
                    }
                }
                channel.force(true);
            }
        } catch (IOException e) {
            DatabaseLogger.logCore(DatabaseLogger.LogLevel.ERROR, "Failed to write meta file: " + file.getName() + " - " + e.getMessage());
        }
    }
}
