package net.weesli.services.user;


import net.weesli.services.Service;
import net.weesli.services.json.JsonBase;
import net.weesli.services.log.DatabaseLogger;
import net.weesli.services.user.enums.UserPermission;
import net.weesli.services.user.model.User;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserRegistry{

    Service service;

    public List<User> admins = new ArrayList<>();

    public UserRegistry(Service service, File usersFile) throws IOException {
        this.service = service;
        JsonBase base = new JsonBase(usersFile);
        List<JsonBase> adminsElement = base.getAsJsonList("admins");
        loadAll(adminsElement);
    }

    private void loadAll(List<JsonBase> adminsElement) {
        for (JsonBase adminNode : adminsElement) {
            try {
                User user = User.fromJson(adminNode);
                admins.add(user);
            } catch (Exception e) {
                DatabaseLogger.logService(DatabaseLogger.LogLevel.ERROR, "Error loading user: " + e.getMessage());
            }
        }
    }

    public User solveUser(String admin){
        String[] splits = admin.split("=");
        String username = splits[0];
        String password = (splits.length > 1) ? splits[1] : "";
        return admins.stream().filter(u -> u.getUsername().equals(username) && u.checkPassword(password)).findFirst().orElse(null);
    }

    public boolean isAdmin(String admin) {
        User user = solveUser(admin);
        return user != null;
    }

    public boolean hasPermission(String admin, UserPermission permission) {
        User user = solveUser(admin);
        return user!= null && user.hasPermission(permission);
    }
}
