package net.weesli.services.log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogWriter {

    private static final Path LOG_PATH;

    static {
        String fileName = "logs-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt";
        Path basePath = Path.of("logs");
        if (!Files.exists(basePath)) {
            try {
                Files.createDirectory(basePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        LOG_PATH = basePath.resolve(fileName);

        try {
            if (!Files.exists(LOG_PATH)) {
                Files.createFile(LOG_PATH);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        try {
            Files.write(
                    LOG_PATH,
                    (LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + " - " + message + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
