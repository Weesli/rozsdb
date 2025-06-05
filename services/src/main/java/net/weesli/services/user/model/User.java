package net.weesli.services.user.model;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.weesli.services.json.JsonBase;
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
    public static User fromJson(JsonBase base) {
        String username = base.get("username").getAsString();
        String password = base.get("password").getAsString();
        List<String> permissionsElement = base.getAsList("permissions", String.class);
        if (permissionsElement == null) {
            throw new IllegalArgumentException("Invalid permissions structure!");
        }
        List<UserPermission> permissions = new ArrayList<>();
        for (String name : permissionsElement) {
            UserPermission permission = UserPermission.valueOf(name);
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
