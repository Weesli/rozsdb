package net.weesli.core.index;

import net.weesli.core.Main;

import java.io.File;
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
        File[] file = Main.core.getDatabasePath().toFile().listFiles();
        if (file != null) { // load all database folders
            for (File path : file) {
                if (path.isDirectory()) {
                    String name = path.getName();
                    indexMetaManagers.put(name, new IndexMetaManager(path));
                }
            }
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
