package net.weesli.server.channel;

import net.weesli.services.ServiceFactory;
import net.weesli.services.json.JsonBase;
import net.weesli.services.user.UserService;

public class ChannelAuth {

    public static boolean auth(JsonBase node){
        if (node.has("user")){
            String user = node.get("user").getAsString();
            UserService userService = getUserService();
            return userService.isAdmin(user);
        }
        return false;
    }

    public static boolean hasPermission(JsonBase node, String permission){
        if (node.has("user")){
            String user = node.get("user").getAsString();
            UserService userService = getUserService();
            return userService.hasPermission(user, permission);
        }
        return false;
    }

    private static UserService getUserService(){
        return (UserService) ServiceFactory.getService(UserService.class);
    }
}
