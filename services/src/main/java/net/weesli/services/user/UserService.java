package net.weesli.services.user;

import lombok.Getter;
import lombok.SneakyThrows;
import net.weesli.services.Service;
import net.weesli.services.ServiceFactory;
import net.weesli.services.user.enums.UserPermission;

import java.io.File;

@Getter
public class UserService extends Service {

   private UserRegistry registry;

    @Override
    public String getName() {
        return "User Service";
    }

    @SneakyThrows
    @Override
    public void startService(Object ...args) {
        registry = new UserRegistry(this, (File) args[0]);
        log(ModuleType.SERVICE, LogLevel.INFO, "User service started");
        ServiceFactory.registerService(this);
    }

    public boolean isAdmin(String admin) {
        return registry.isAdmin(admin);
    }

    public boolean hasPermission(String admin, String permission){
        return switch (permission) {
            case "write" -> {
                boolean isAdmin = registry.hasPermission(admin, UserPermission.ADMIN);
                yield isAdmin || registry.hasPermission(admin, UserPermission.WRITE);
            }
            case "read" -> {
                boolean isAdminOrWrite = registry.hasPermission(admin, UserPermission.ADMIN);
                yield isAdminOrWrite || registry.hasPermission(admin, UserPermission.READ);
            }
            default -> false;
        };
    }
}
