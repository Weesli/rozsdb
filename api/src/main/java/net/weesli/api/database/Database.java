package net.weesli.api.database;

import java.io.File;
import java.util.List;

public interface Database {

    String getName();
    File getDirectory();

    List<Collection> getCollections();
}
