package net.weesli.services.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseLogger {

    public enum LogLevel {
        INFO, WARN, ERROR
    }
    public enum ModuleType {
        CORE, SERVICE, SERVER
    }

    public static void log(ModuleType moduleType, LogLevel level, String message) {
        String color = switch (level) {
            case WARN -> "\u001B[33m";
            case ERROR -> "\u001B[31m";
            default -> "\u001B[0m";
        };
        System.out.printf("%s[%s] [%s] [%s] %s\u001B[0m%n", color,moduleType.name(), level.name(), getFormattedTime(), message);
    }

    private static String getFormattedTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
