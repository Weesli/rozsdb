package net.weesli.services.user.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
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

    public static User fromJson(String json) {
        JsonObject user = JsonParser.parseString(json).getAsJsonObject();
        String username = user.get("username").getAsString();
        String password = user.get("password").getAsString();
        JsonElement permissionsElement = user.get("permissions");
        List<UserPermission> permissions = permissionsElement.getAsJsonArray().asList().stream().map(jsonElement -> UserPermission.valueOf(jsonElement.getAsString())).toList();
        return new User(username, password, new ArrayList<>(permissions));
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public boolean hasPermission(UserPermission permission) {
        return permissions.contains(permission);
    }
}
