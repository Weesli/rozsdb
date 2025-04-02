package net.weesli.api;

import net.weesli.api.model.ObjectId;

import java.util.concurrent.ConcurrentHashMap;

public interface CacheStore {

    ConcurrentHashMap<ObjectId, String> getCache();
}
