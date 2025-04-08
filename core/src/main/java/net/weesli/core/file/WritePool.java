package net.weesli.core.file;

import lombok.Getter;
import net.weesli.api.DatabasePool;
import net.weesli.api.database.Collection;
import net.weesli.api.database.Database;
import net.weesli.api.model.ObjectId;
import net.weesli.core.database.DatabaseImpl;
import net.weesli.core.model.WriteTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

@Getter
public class WritePool extends DatabaseFileManager implements DatabasePool {

    private final ScheduledExecutorService scheduler;
    private final ScheduledExecutorService threadPool;

    private List<Database> databases = new ArrayList<>();
    private final BlockingQueue<WriteTask> writeQueue;
    private final int BATCH_SIZE = 100;

    public WritePool() {
        scheduler = Executors.newScheduledThreadPool(1);
        threadPool = Executors.newScheduledThreadPool(10);
        writeQueue = new LinkedBlockingQueue<>();
        start();
    }

    public void register(DatabaseImpl databaseImpl) {
        databases.add(databaseImpl);
    }

    public void start() {
        Runnable runnable = () -> {
            threadPool.submit(this::processWriteQueue);
        };
        scheduler.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MINUTES);
    }
    private void processWriteQueue() {
        List<WriteTask> batch = new ArrayList<>();
        writeQueue.drainTo(batch, BATCH_SIZE);

        if (!batch.isEmpty()) {
            batch.forEach(task -> {
                for (Collection collection : task.database().getCollections()) {
                    File file = new File(collection.getCollectionPath().toFile(), task.objectId() + "");
                    write(task.data(), file);
                }
            });
        }
    }

    public void enqueueWrite(Database database, ObjectId objectId, byte[] data) {
        WriteTask task = new WriteTask(database, objectId, data);
        try {
            writeQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to enqueue write task", e);
        }
    }

    public void stop() {
        scheduler.shutdown();
        threadPool.shutdown();
    }

    public void forceUpdate() {
        List<WriteTask> allTasks = new ArrayList<>();
        writeQueue.drainTo(allTasks);

        allTasks.forEach(task -> {
            for (Collection collection : task.database().getCollections()) {
                File file = new File(collection.getCollectionPath().toFile(), task.objectId() + "");
                write(task.data(), file);
            }
        });
    }
}
