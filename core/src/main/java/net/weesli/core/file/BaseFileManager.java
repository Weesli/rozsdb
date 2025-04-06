package net.weesli.core.file;

import com.github.luben.zstd.Zstd;
import lombok.SneakyThrows;
import net.weesli.services.log.DatabaseLogger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class BaseFileManager {
    private final ExecutorService executorService;

    // Constructor with configurable thread pool
    public BaseFileManager(int threadPoolSize) {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    // Default constructor with reasonable default thread pool size
    public BaseFileManager() {
        this(Runtime.getRuntime().availableProcessors());
    }

    @SneakyThrows
    public synchronized void write(byte[] src, File file) {
        if (!file.exists()) {
            file.createNewFile();
        }
        try (FileOutputStream fis = new FileOutputStream(file)) {
            fis.write(src);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public byte[] readWithSizeCheck(File file, int maxSizeMB) {
        if (!file.isFile()) throw new IllegalArgumentException("Path is not a file");

        long fileSizeBytes = file.length();
        long maxSizeBytes = maxSizeMB * 1024L * 1024L;
        if (fileSizeBytes > maxSizeBytes) return null;

        try (
                FileInputStream fis = new FileInputStream(file);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file.getName(), e);
        }
    }

    public Map<String, byte[]> readAllFilesInDirectory(File directory, int maxSizePerFileMB) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("The provided path is not a directory.");
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return Map.of();
        }

        ConcurrentMap<String, byte[]> results = new ConcurrentHashMap<>();
        List<Callable<Void>> tasks = getCallables(maxSizePerFileMB, files, results);

        // Execute all tasks and wait for completion
        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("File processing was interrupted", e);
        }

        return new HashMap<>(results);
    }

    private @NotNull List<Callable<Void>> getCallables(int maxSizePerFileMB, File[] files, ConcurrentMap<String, byte[]> results) {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                tasks.add(() -> {
                    try {
                        byte[] content = readWithSizeCheck(file, maxSizePerFileMB);
                        if (content != null) {
                            results.put(file.getName(), content);
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
                    }
                    return null;
                });
            }
        }
        return tasks;
    }

    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void deleteFile(File file) {
        if (!file.delete()) {
            DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.ERROR, file.toPath() + " not deleted!");
        }
    }
}