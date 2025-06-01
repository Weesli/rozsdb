package net.weesli.core.timeout.types;

import net.weesli.api.database.Collection;
import net.weesli.core.timeout.TimeoutTask;

public class CollectionTimeoutTask extends TimeoutTask {

    private final Collection collection;

    public CollectionTimeoutTask(Collection collection) {
        super(600 * 1000);
        this.collection = collection;
        run();
    }

    @Override
    public void execute() {
        // save cache to disk and clear cache's
        collection.save();
        collection.getCollectionData().getDataStore().clear();
        collection.getDatabase().unregisterCollection(collection.getCollectionName());
        cancel();
    }
}
