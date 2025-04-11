package net.weesli.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.luben.zstd.Zstd;
import net.weesli.core.mapper.ObjectMapperProvider;
import net.weesli.services.log.DatabaseLogger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;

public class IndexMetaUtil {

    public static boolean insertDefaultMeta(File file){
        if (!file.exists()) {
            try {
                file.createNewFile();
                JsonNode node = getBaseMeta();
                IndexMetaUtil.writeMeta(file, node);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static JsonNode getBaseMeta(){
        ObjectMapper mapper = ObjectMapperProvider.getInstance();
        ObjectNode node = mapper.createObjectNode();
        node.put("records", mapper.createArrayNode());
        return node;
    }

    public static JsonNode getMeta(File file) {
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            long fileSize = channel.size();
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            byte[] value = new byte[(int) fileSize];
            buffer.get(value);

            long decompressedSize = Zstd.decompressedSize(value);
            if (decompressedSize <= 0) {
                decompressedSize = value.length * 10L;
            }

            byte[] decompressed = Zstd.decompress(value, (int) decompressedSize);
            ObjectMapper mapper = ObjectMapperProvider.getInstance();
            JsonNode root = mapper.readTree(new String(decompressed));

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
            DatabaseLogger.logCore(DatabaseLogger.LogLevel.ERROR,
                    "Failed to read meta file: " + file.getName() + " - " + e.getMessage());
        }

        return null;
    }




    public static void writeMeta(File file, JsonNode node) {
        try {
            ObjectMapper mapper = ObjectMapperProvider.getInstance();
            byte[] compressed = Zstd.compress(mapper.writeValueAsBytes(node));
            try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                ByteBuffer buffer = ByteBuffer.wrap(compressed);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
        } catch (IOException e) {
            DatabaseLogger.logCore(DatabaseLogger.LogLevel.ERROR, "Failed to write meta file: " + file.getName() + " - " + e.getMessage());
        }
    }
}
