package net.weesli.core.file;

import lombok.Getter;
import net.weesli.api.DatabasePool;
import net.weesli.api.database.Collection;
import net.weesli.api.database.Database;
import net.weesli.api.model.ObjectId;
import net.weesli.core.database.DatabaseImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
@Getter
public class WritePool extends BaseFileManager implements DatabasePool {

    private final ScheduledExecutorService scheduler;
    private final ScheduledExecutorService threadPool;

    private List<Database> databases = new ArrayList<>();

    public WritePool() {
        scheduler = Executors.newScheduledThreadPool(1);
        threadPool = Executors.newScheduledThreadPool(10);
        start();
    }

    public void register(DatabaseImpl databaseImpl) {
        databases.add(databaseImpl);
    }

    public void start() {
        Runnable runnable = () -> {
            for (Database database : databases) {
                threadPool.submit(() -> applyDatabase(database));
            }
        };
        scheduler.scheduleWithFixedDelay(runnable, 0, 1, TimeUnit.MINUTES);
    }

    private void applyDatabase(Database database) {
        for (Collection collection : database.getCollections()) {
            for (Map.Entry<ObjectId, String> entry : collection.getCache().entrySet()) {
                File file = new File(collection.getCollectionPath().toFile(), entry.getKey() + ".json");
                write(entry.getValue(), file);
            }
        }
    }

    public void stop() {
        scheduler.shutdown();
        threadPool.shutdown();
    }

    public void forceUpdate() {
        for (Database database : databases) {
            for (Collection collection : database.getCollections()) {
                for (Map.Entry<ObjectId, String> entry : collection.getCache().entrySet()) {
                    File file = new File(collection.getCollectionPath().toFile(), entry.getKey() + ".json");
                    write(entry.getValue(), file);
                }
            }
        }
    }
}
