package net.weesli.core.database;

import net.weesli.api.DatabasePool;
import net.weesli.api.DatabasePoolProvider;
import net.weesli.core.Main;

public class DatabasePoolProviderImpl implements DatabasePoolProvider { // this class giving to server module for drivers
    @Override
    public DatabasePool getDatabasePool() {
        return Main.getCore().getWritePool();
    }
}
