package net.weesli.api;

import net.weesli.api.database.Database;

import java.util.List;

public interface DatabaseProvider {

    List<Database> getDatabases();
    CoreSettings getCoreSettings();
}
