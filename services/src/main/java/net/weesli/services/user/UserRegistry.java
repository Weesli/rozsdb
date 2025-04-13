package net.weesli.services.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.weesli.services.Service;
import net.weesli.services.log.DatabaseLogger;
import net.weesli.services.mapper.ObjectMapperProvider;
import net.weesli.services.user.enums.UserPermission;
import net.weesli.services.user.model.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class UserRegistry extends BaseUser{

    Service service;
    ObjectMapper mapper = ObjectMapperProvider.getInstance();

    public UserRegistry(Service service, File usersFile) throws IOException {
        this.service = service;
        JsonNode node = mapper.readTree(usersFile);
        JsonNode adminsElement = node.get("admins");
        if (adminsElement == null || !adminsElement.isArray()) {
            throw new IllegalArgumentException("Invalid admins structure!");
        }
        loadAll(adminsElement);
    }

    private void loadAll(JsonNode adminsElement) {
        for (JsonNode adminNode : adminsElement) {
            String json = adminNode.toString();
            try {
                User user = User.fromJson(json);
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
