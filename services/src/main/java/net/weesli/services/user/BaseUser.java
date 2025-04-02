package net.weesli.services.user;

import net.weesli.services.user.model.User;

import java.util.ArrayList;
import java.util.List;

public class BaseUser {
    public List<User> admins;

    public BaseUser() {
        admins = new ArrayList<>();
    }

}
