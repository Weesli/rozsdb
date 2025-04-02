package net.weesli.services;

import java.util.concurrent.ConcurrentHashMap;

public class ServiceFactory {

    private static final ConcurrentHashMap<Class<?>, Service> REGISTERED_SERVICES = new ConcurrentHashMap<>();

    public static Object getService(Class<? extends Service> service){
        return REGISTERED_SERVICES.get(service);
    }

    public static void registerService(Service service){
        REGISTERED_SERVICES.put(service.getClass(), service);
    }
}
