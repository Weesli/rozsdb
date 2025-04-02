package net.weesli.api;

import net.weesli.api.database.Database;

import java.util.List;

public interface DatabasePool {

    List<Database> getDatabases();
}
