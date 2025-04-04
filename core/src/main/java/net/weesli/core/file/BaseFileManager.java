package net.weesli.core.file;

import lombok.SneakyThrows;
import net.weesli.services.log.DatabaseLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
    public synchronized void write(String src, File file) {
        if (!file.exists()) {
            file.createNewFile();
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(src);
            writer.flush();
        }
    }

    /**
     * Reads a file line by line without loading the entire file into memory.
     * @param file The file to read
     * @param lineProcessor A consumer function that processes each line
     */
    public void readLineByLine(File file, Consumer<String> lineProcessor) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("The provided path is not a file.");
        }

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineProcessor.accept(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file.getName(), e);
        }
    }

    /**
     * Read a file with a size check to avoid large files
     * @param file The file to read
     * @param maxSizeMB Maximum file size in MB to read fully into memory
     * @return File contents as a string, or null if file exceeds size limit
     */
    public String readWithSizeCheck(File file, int maxSizeMB) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("The provided path is not a file.");
        }

        // Check file size
        long fileSizeBytes = file.length();
        long maxSizeBytes = maxSizeMB * 1024L * 1024L;

        if (fileSizeBytes > maxSizeBytes) {
            System.out.printf("File %s is too large (%d MB) - exceeds limit of %d MB%n",
                    file.getName(), fileSizeBytes / (1024 * 1024), maxSizeMB);
            return null;  // Skip this file
        }

        // If file is within size limit, read it normally
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file.getName(), e);
        }

        return content.toString();
    }

    /**
     * Process all files in a directory with memory safety using multiple threads
     * @param directory The directory to process
     * @param maxSizePerFileMB Maximum size per file in MB
     * @return Map of filename to file content
     */
    public Map<String, String> readAllFilesInDirectory(File directory, int maxSizePerFileMB) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("The provided path is not a directory.");
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return Map.of();
        }

        ConcurrentMap<String, String> results = new ConcurrentHashMap<>();

        // Create a list of tasks
        List<Callable<Void>> tasks = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                tasks.add(() -> {
                    try {
                        String content = readWithSizeCheck(file, maxSizePerFileMB);
                        if (content != null) {
                            results.put(file.getName().replace(".json", ""), content);
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
                    }
                    return null;
                });
            }
        }

        // Execute all tasks and wait for completion
        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("File processing was interrupted", e);
        }

        return new HashMap<>(results);
    }

    /**
     * Process a large directory in batches to avoid memory issues using multiple threads
     * @param directory The directory to process
     * @param batchProcessor Function to process each batch of files
     * @param batchSize Number of files per batch
     * @param maxSizePerFileMB Maximum size per file in MB
     */
    public void processBatchesOfFiles(
            File directory,
            Consumer<Map<String, String>> batchProcessor,
            int batchSize,
            int maxSizePerFileMB) {

        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("The provided path is not a directory.");
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        // Create a queue of files to process
        BlockingQueue<File> fileQueue = new LinkedBlockingQueue<>(Arrays.asList(files));
        AtomicInteger totalProcessed = new AtomicInteger(0);
        int totalFiles = files.length;

        // Number of workers based on thread pool size
        int numWorkers = ((ThreadPoolExecutor) executorService).getMaximumPoolSize();
        List<Future<?>> futures = new ArrayList<>(numWorkers);

        // Create worker tasks
        for (int i = 0; i < numWorkers; i++) {
            futures.add(executorService.submit(() -> {
                Map<String, String> batch = new HashMap<>();
                int localCount = 0;

                while (true) {
                    File file = fileQueue.poll();
                    if (file == null) break; // No more files to process

                    if (!file.isFile()) continue;

                    try {
                        String content = readWithSizeCheck(file, maxSizePerFileMB);
                        if (content != null) {
                            synchronized (batch) {
                                batch.put(file.getName(), content);
                                localCount++;
                            }
                        }

                        // Check if batch is full
                        if (localCount >= batchSize) {
                            synchronized (batch) {
                                if (!batch.isEmpty()) {
                                    batchProcessor.accept(batch);
                                    int processed = totalProcessed.addAndGet(localCount);
                                    System.out.printf("Processed batch of %d files (total: %d/%d)%n",
                                            localCount, processed, totalFiles);
                                    batch = new HashMap<>();
                                    localCount = 0;
                                    System.gc(); // Suggest garbage collection
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
                    }
                }

                // Process remaining files in the last batch
                synchronized (batch) {
                    if (!batch.isEmpty()) {
                        batchProcessor.accept(batch);
                        int processed = totalProcessed.addAndGet(localCount);
                        System.out.printf("Processed final batch of %d files (total: %d/%d)%n",
                                localCount, processed, totalFiles);
                    }
                }
            }));
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("File processing was interrupted", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Error during file processing", e.getCause());
            }
        }
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