package net.weesli.core.index;

import net.weesli.api.database.Database;
import net.weesli.core.Main;
import net.weesli.core.database.DatabasePool;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class IndexManager {
    private final ConcurrentHashMap<String, IndexMetaManager> indexMetaManagers = new ConcurrentHashMap<>();

    private static IndexManager instance;

    private IndexManager() {
        load();
    }

    public static IndexManager getInstance() {
        if (instance == null) {
            instance = new IndexManager();
        }
        return instance;
    }

    public void load() {
        List<Database> database = DatabasePool.getInstance().getDatabases();
        for (Database db : database) {
            indexMetaManagers.put(db.getName(), new IndexMetaManager(db.getDirectory()));
        }
    }

    public IndexMetaManager getIndexMetaManager(String database) {
        return indexMetaManagers.get(database);
    }

    public void saveAll(){
        indexMetaManagers.forEach((key, value) -> {
            value.saveRecords(new File(Main.core.getDatabasePath().toFile(), key));
        });
    }
}
