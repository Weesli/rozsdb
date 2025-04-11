package net.weesli.core.file;

import lombok.SneakyThrows;
import net.weesli.api.database.Collection;
import net.weesli.api.model.ObjectId;
import net.weesli.services.log.DatabaseLogger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class DatabaseFileManager {
    private final ExecutorService executorService;
    private static final int SMALL_FILE_THRESHOLD = 1024 * 1024;

    public DatabaseFileManager(int threadPoolSize) {
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    public DatabaseFileManager() {
        this(Runtime.getRuntime().availableProcessors() * 2);
    }

    @SneakyThrows
    public synchronized void write(byte[] src, File file) {
        if (!file.exists()) {
            file.createNewFile();
        }

        try (FileChannel channel = FileChannel.open(file.toPath(),
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer buffer = ByteBuffer.wrap(src);

            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        } catch (IOException e) {
            DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.ERROR,
                    "Failed to write file: " + file.getName() + " - " + e.getMessage());
            throw e;
        }
    }
    public byte[] readWithSizeCheck(File file, int maxSizeMB) {
        if (!file.isFile()) throw new IllegalArgumentException("Path is not a file");

        long fileSizeBytes = file.length();
        long maxSizeBytes = maxSizeMB * 1024L * 1024L;
        if (fileSizeBytes > maxSizeBytes) return null;

        if (fileSizeBytes < SMALL_FILE_THRESHOLD) {
            return readSmallFile(file);
        } else {
            return readLargeFile(file);
        }
    }
    private byte[] readSmallFile(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file.getName(), e);
        }
    }

    private byte[] readLargeFile(File file) {
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            long fileSize = channel.size();

            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            byte[] result = new byte[(int) fileSize];
            buffer.get(result);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read large file: " + file.getName(), e);
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

        Map<Boolean, List<File>> partitionedFiles = Arrays.stream(files)
                .filter(File::isFile)
                .collect(Collectors.partitioningBy(f -> f.length() < SMALL_FILE_THRESHOLD));

        ConcurrentMap<String, byte[]> results = new ConcurrentHashMap<>();

        CompletableFuture<Void> smallFilesFuture = processFilesInParallel(
                partitionedFiles.get(true), maxSizePerFileMB, results);

        CompletableFuture<Void> largeFilesFuture = processLargeFiles(
                partitionedFiles.get(false), maxSizePerFileMB, results);

        try {
            CompletableFuture.allOf(smallFilesFuture, largeFilesFuture).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("File processing was interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error processing files", e.getCause());
        }

        return new HashMap<>(results);
    }

    private CompletableFuture<Void> processFilesInParallel(List<File> files, int maxSizePerFileMB,
                                                           ConcurrentMap<String, byte[]> results) {
        List<CompletableFuture<Void>> futures = files.stream()
                .map(file -> CompletableFuture.runAsync(() -> {
                    try {
                        if (file.getName().contains("meta")) return;
                        byte[] content = readWithSizeCheck(file, maxSizePerFileMB);
                        if (content != null) {
                            results.put(file.getName(), content);
                        }
                    } catch (Exception e) {
                        DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.ERROR,
                                "Error processing file " + file.getName() + ": " + e.getMessage());
                    }
                }, executorService))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> processLargeFiles(List<File> files, int maxSizePerFileMB,
                                                      ConcurrentMap<String, byte[]> results) {
        int maxConcurrent = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
        Semaphore semaphore = new Semaphore(maxConcurrent);

        List<CompletableFuture<Void>> futures = files.stream()
                .map(file -> CompletableFuture.runAsync(() -> {
                    try {
                        if (file.getName().contains("meta")) return;
                        semaphore.acquire();
                        byte[] content = readWithSizeCheck(file, maxSizePerFileMB);
                        if (content != null) {
                            results.put(file.getName(), content);
                        }
                    } catch (Exception e) {
                        DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.ERROR,
                                "Error processing large file " + file.getName() + ": " + e.getMessage());
                    } finally {
                        semaphore.release();
                    }
                }, executorService))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public void writeCollection(Collection collection) throws IOException {
        List<CompletableFuture<Void>> futures = getCompletableFutures(collection);
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Collection write was interrupted", e);
        } catch (ExecutionException e) {
            throw new IOException("Collection write failed", e.getCause());
        }
    }

    private @NotNull List<CompletableFuture<Void>> getCompletableFutures(Collection collection) {
        File collectionDir = collection.getCollectionPath().toFile();
        if (!collectionDir.exists()) {
            collectionDir.mkdirs();
        }

        Map<ObjectId, byte[]> entries = collection.getCache();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        entries.forEach((key, value) -> {
            File targetFile = new File(collectionDir, key.getObjectId().toString());
            if (targetFile.getName().contains("meta")) return;
            futures.add(CompletableFuture.runAsync(() -> {
                writeIfChanged(value, targetFile);
            }, executorService));
        });
        return futures;
    }


    @SneakyThrows
    public synchronized void writeIfChanged(byte[] src, File file) {
        byte[] existingData = readWithSizeCheck(file, Integer.MAX_VALUE);

        if (existingData == null || !Arrays.equals(existingData, src)) {
            if (!file.exists()) {
                file.createNewFile();
            }
            write(src, file);
        } else {
            DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.INFO,
                    "No changes detected for file: " + file.getName() + ", skipping write.");
        }
    }


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

    public boolean deleteFile(File file) {
        try {
            Files.deleteIfExists(file.toPath());
            return true;
        } catch (IOException e) {
            DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.ERROR,
                    file.toPath() + " not deleted: " + e.getMessage());
            return false;
        }
    }
}