package net.weesli.services.security.ip;

import lombok.SneakyThrows;
import net.weesli.services.security.ip.util.IPUtil;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public class IpChecker {

    public IpChecker(){

    }

    @SneakyThrows
    public boolean checkIp(String ip){
        if(ip == null || ip.isEmpty()){
            return false;
        }
        List<String> allowedIps = IPUtil.getAllowedIps(
                IPUtil.getCache(new File("config/security.json").toPath())
        );
        if(allowedIps == null){
            return false;
        }
        if (allowedIps.contains("@")){ // Allow all IPs if "@" is present in the list
            return true;
        }
        return allowedIps.contains(ip);
    }
}
