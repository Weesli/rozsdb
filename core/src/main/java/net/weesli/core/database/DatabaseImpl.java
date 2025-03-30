package net.weesli.core.database;

import lombok.Getter;
import net.weesli.api.database.Collection;
import net.weesli.api.database.Database;
import net.weesli.services.log.DatabaseLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
@Getter
public class DatabaseImpl implements Database {

    private String name;
    private File directory;

    private List<Collection> collections = new ArrayList<>();

    public void addCollection(CollectionImpl collection) {
        collections.add(collection);
    }

    public static DatabaseImpl load(File file) {
        if (!file.exists()) {
            DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.ERROR, "Database directory not found: " + file.getAbsolutePath());
            return null;
        }
        if (!file.isDirectory()) return null;

        DatabaseImpl databaseImpl = new DatabaseImpl();
        databaseImpl.name = file.getName();
        databaseImpl.directory = file;

        for (File path : file.listFiles()) {
            if (path.isDirectory()) {
                String name = path.getName();
                databaseImpl.addCollection(new CollectionImpl(databaseImpl, name));
            } else {
                DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.ERROR, "Invalid file found in database directory: " + path.getAbsolutePath());
            }
        }
        return databaseImpl;
    }
}
