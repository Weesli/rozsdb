package net.weesli.core.database;

import lombok.Getter;
import net.weesli.api.database.Collection;
import net.weesli.api.database.Database;
import net.weesli.services.log.DatabaseLogger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
public class DatabaseImpl implements Database {

    private final String name;
    private final File directory;

    private final List<Collection> collections = new ArrayList<>();
    private final List<String> collectionNames = new ArrayList<>();

    public DatabaseImpl(String name, @NotNull File directory) {
        this.name = name;
        this.directory = directory;
        for (File path : Objects.requireNonNull(directory.listFiles())) {
            if (path.isDirectory()) {
                collectionNames.add(path.getName());
            } else {
                DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.ERROR, "Invalid file found in database directory: " + path.getAbsolutePath());
            }
        }
    }

    public void addCollection(CollectionImpl collection) {
        collections.add(collection);
    }

    public static DatabaseImpl loadAll(File file) {
        if (!file.exists()) {
            DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.ERROR, "Database directory not found: " + file.getAbsolutePath());
            return null;
        }
        if (!file.isDirectory()) return null;
        DatabaseImpl databaseImpl = new DatabaseImpl(file.getName(), file);
        for (File path : Objects.requireNonNull(file.listFiles())) {
            if (path.isDirectory()) {
                String name = path.getName();
                databaseImpl.addCollection(new CollectionImpl(databaseImpl, name));
            } else {
                DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.ERROR, "Invalid file found in database directory: " + path.getAbsolutePath());
            }
        }
        return databaseImpl;
    }

    public void load(String collectionName){
        addCollection(new CollectionImpl(this, collectionName));
    }

    public Collection getCollection(String name){
        if (collectionNames.contains(name)){ // does this collection really exist in this database
            Optional<Collection> collection = collections.stream().filter(c -> c.getCollectionName().equals(name)).findFirst();
            if (collection.isPresent()){ // if it is currently registered, give it directly in the registered format
                return collection.get();
            }else { // if it is not registered, load collection and again try
                load(name);
                return getCollection(name);
            }
        }
        return null;
    }

    @Override
    public void unregisterCollection(String collectionName) {
        Optional<Collection> collection = collections.stream().filter(c -> c.getCollectionName().equals(collectionName)).findFirst();
        if (collection.isPresent()){
            collections.remove(collection.get());
        }
        else {
            DatabaseLogger.log(DatabaseLogger.ModuleType.CORE, DatabaseLogger.LogLevel.ERROR, "Collection not found: " + collectionName);
        }
    }
}
