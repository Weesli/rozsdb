package net.weesli.core.database;

import net.weesli.api.DatabasePool;
import net.weesli.api.DatabasePoolProvider;
import net.weesli.core.Main;

public class DatabasePoolProviderImpl implements DatabasePoolProvider {
    @Override
    public DatabasePool getDatabasePool() {
        return Main.getCore().getWritePool();
    }
}
