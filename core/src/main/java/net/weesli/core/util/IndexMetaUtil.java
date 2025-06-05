package net.weesli.core.util;

import net.weesli.services.json.JsonBase;
import net.weesli.services.log.DatabaseLogger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;

public class IndexMetaUtil {

    public static boolean insertDefaultMeta(File file){
        if (!file.exists()) {
            JsonBase meta = getBaseMeta();
            writeMeta(file, meta);
        }
        return true;
    }

    private static JsonBase getBaseMeta(){
        JsonBase base = new JsonBase(new HashMap<>());
        base.put("records", new ArrayList<>());
        return base;
    }

    public static JsonBase getMeta(File file) {
        try {
            if (!file.exists() || file.length() == 0) {
                return null;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] decompressed = CompressUtil.decompress(bytes);
            JsonBase base =  new JsonBase(decompressed);
            return base;
        } catch (IOException e) {
            DatabaseLogger.logCore(DatabaseLogger.LogLevel.ERROR, "Failed to read meta file: " + file.getName() + " - " + e.getMessage());
        }
        return null;
    }

    public static void writeMeta(File file, JsonBase node) {
        try {
            byte[] compressed = CompressUtil.compress(node.asJsonText().getBytes(StandardCharsets.UTF_8));
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
