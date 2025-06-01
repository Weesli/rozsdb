package net.weesli.core.database;

import net.weesli.api.database.Database;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabasePool {
    private static DatabasePool instance;

    private static final ConcurrentHashMap<String, Database> dataStore = new ConcurrentHashMap<>();

    public static DatabasePool getInstance() {
        if (instance == null) {
            instance = new DatabasePool();
        }
        return instance;
    }

    public Database load(String name) {
        Database database = dataStore.get(name);
        if (database == null) {
            database = new DatabaseImpl(name, new File(name));
            dataStore.put(name, database);
        }
        return database;
    }

    public Database load(File file) {
        Database database = dataStore.get(file.getName());
        if (database == null) {
            database = new DatabaseImpl(file.getName(), file);
            dataStore.put(file.getName(), database);
        }
        return database;
    }

    public List<Database> getDatabases() {
        return List.copyOf(dataStore.values());
    }

    public Map<String, Database> getDatabasesMap() {
        return dataStore;
    }

}

