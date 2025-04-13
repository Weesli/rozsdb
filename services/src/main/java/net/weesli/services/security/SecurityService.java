package net.weesli.services.security;

import lombok.Getter;
import net.weesli.services.Service;
import net.weesli.services.ServiceFactory;
import net.weesli.services.log.DatabaseLogger;
import net.weesli.services.security.ip.IpChecker;

@Getter
public class SecurityService extends Service {

    private IpChecker ipChecker;

    @Override
    public String getName() {
        return "SecurityService";
    }

    @Override
    public void startService(Object... args) {
        ipChecker = new IpChecker();
        logService(DatabaseLogger.LogLevel.INFO, "Security service started");
        ServiceFactory.registerService(this);
    }
}
