package net.weesli.server.channel;

import net.weesli.services.ServiceFactory;
import net.weesli.services.security.SecurityService;

public class ChannelSecurity {

    public static boolean isAllowedIp(String ip) {
        return getSecurityService().getIpChecker().checkIp(ip);
    }

    private static SecurityService getSecurityService(){
        return (SecurityService) ServiceFactory.getService(SecurityService.class);
    }
}
