package net.weesli.core.database;

import net.weesli.api.CoreSettings;
import net.weesli.api.DatabaseProvider;
import net.weesli.api.database.Database;
import net.weesli.core.Main;

import java.util.List;

public class DatabaseProviderImpl implements DatabaseProvider { // this class giving to server module for drivers
    @Override
    public List<Database> getDatabases() {
        return DatabasePool.getInstance().getDatabases();
    }

    @Override
    public CoreSettings getCoreSettings() {
        return Main.core.getSettings();
    }
}
