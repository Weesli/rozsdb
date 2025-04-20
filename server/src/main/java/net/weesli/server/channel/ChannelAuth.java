package net.weesli.server.channel;

import com.fasterxml.jackson.databind.JsonNode;
import net.weesli.services.ServiceFactory;
import net.weesli.services.user.UserService;

public class ChannelAuth {

    public static boolean auth(JsonNode node){
        if (node.has("user")){
            String user = node.get("user").asText();
            UserService userService = getUserService();
            return userService.isAdmin(user);
        }
        return false;
    }

    public static boolean hasPermission(JsonNode node, String permission){
        if (node.has("user")){
            String user = node.get("user").asText();
            UserService userService = getUserService();
            return userService.hasPermission(user, permission);
        }
        return false;
    }

    private static UserService getUserService(){
        return (UserService) ServiceFactory.getService(UserService.class);
    }
}
