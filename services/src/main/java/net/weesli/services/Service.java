package net.weesli.services;

import net.weesli.services.log.DatabaseLogger;

public abstract class Service extends DatabaseLogger{

    public abstract String getName();
    public abstract void startService(Object... args);
}
