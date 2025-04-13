package net.weesli.services.user.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.weesli.services.mapper.ObjectMapperProvider;
import net.weesli.services.user.enums.UserPermission;

import java.util.ArrayList;
import java.util.List;

@Getter@Setter
public class User {
    private String username;
    private String password;
    private List<UserPermission> permissions;
    public User(String username, String password, List<UserPermission> permissions) {
        this.username = username;
        this.permissions = permissions;
        this.password = password;
    }

    @SneakyThrows
    public static User fromJson(String json) {
        ObjectMapper mapper = ObjectMapperProvider.getInstance();
        JsonNode node = mapper.readTree(json);
        String username = node.get("username").asText();
        String password = node.get("password").asText();
        JsonNode permissionsElement = node.get("permissions");
        if (permissionsElement == null || !permissionsElement.isArray()) {
            throw new IllegalArgumentException("Invalid permissions structure!");
        }
        List<UserPermission> permissions = new ArrayList<>();
        for (JsonNode permissionNode : permissionsElement) {
            String permissionName = permissionNode.asText();
            UserPermission permission = UserPermission.valueOf(permissionName);
            permissions.add(permission);
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Invalid username structure!");
        }
        if (password == null) {
            throw new IllegalArgumentException("Invalid password structure!");
        }
        return new User(username, password, new ArrayList<>(permissions));
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public boolean hasPermission(UserPermission permission) {
        return permissions.contains(permission);
    }
}
