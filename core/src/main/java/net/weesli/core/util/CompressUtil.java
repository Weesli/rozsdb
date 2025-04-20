package net.weesli.core.util;

import com.github.luben.zstd.Zstd;
import lombok.SneakyThrows;
import net.weesli.core.file.DatabaseFileManager;

import java.io.File;
import java.nio.file.Path;

public class CompressUtil {
    public static byte[] compress(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data to compress cannot be null or empty");
        }
        // compress the data
        byte[] compressed = Zstd.compress(data);
        if (compressed == null) {
            throw new IllegalArgumentException("Failed to compress data");
        }
        return compressed;
    }

    @SneakyThrows
    public static byte[] decompress(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getPath());
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + file.getPath());
        }
        if (file.length() == 0) {
            throw new IllegalArgumentException("File is empty: " + file.getPath());
        }
        return decompress(DatabaseFileManager.read(file));
    }

    @SneakyThrows
    public static byte[] decompress(byte[] bytes){
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Data to decompress cannot be null or empty");
        }
        // decompress the data
        long decompressedSize = Zstd.getFrameContentSize(bytes);
        byte[] decompressed = Zstd.decompress(bytes, (int) decompressedSize);
        if (decompressed == null) {
            throw new IllegalArgumentException("Failed to decompress data");
        }
        return decompressed;
    }
}
