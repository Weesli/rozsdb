package net.weesli.core;

import lombok.Getter;
import net.weesli.core.database.DatabaseImpl;
import net.weesli.core.database.DatabasePoolProviderImpl;
import net.weesli.core.index.IndexManager;
import net.weesli.core.util.Settings;
import net.weesli.core.file.WritePool;
import net.weesli.server.Server;
import net.weesli.services.log.DatabaseLogger;
import net.weesli.services.security.SecurityService;
import net.weesli.services.user.UserService;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

    @Getter public static MainInstance core;

    public static void main(String[] args) {
        try {
            core = new MainInstance();
            core.createDatabase();
            core.createWritePool();
            IndexManager.getInstance();
            new Server(new DatabasePoolProviderImpl(), core.getSettings().get("port").asInt());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { // register a hook for force save
                if (core.writePool != null) core.writePool.forceUpdate();
                IndexManager.getInstance().saveAll();
                DatabaseLogger.log(DatabaseLogger.ModuleType.CORE,DatabaseLogger.LogLevel.INFO, "RozsDatabase is shutting down...");
            }));
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Getter
    public static class MainInstance extends DatabaseLogger{

        private Path databasePath;
        private WritePool writePool;
        private Settings settings;

        public MainInstance() throws InterruptedException {
            log(ModuleType.CORE, DatabaseLogger.LogLevel.INFO, "Starting RozsDatabase...");
            Thread.sleep(500);
            initializeAllFiles();
            settings = new Settings(new File("config/settings.json"));
            startServices(settings);
        }

        public void createDatabase() {
            log(ModuleType.CORE, DatabaseLogger.LogLevel.INFO, "Loading main database file...");
            File file = new File("database");
            if (!file.exists()) {
                file.mkdirs();
                log(ModuleType.CORE, DatabaseLogger.LogLevel.INFO, "Database file is created!");
            }
            databasePath = file.toPath();
            log(ModuleType.CORE, DatabaseLogger.LogLevel.INFO, "Loading WritePool...");
        }

        private void startServices(Settings settings){
            log(ModuleType.CORE, DatabaseLogger.LogLevel.INFO, "All Services Loading...");
            new UserService().startService(new File("config/admins.json"));
            new SecurityService().startService();
        }

        public void createWritePool() {
            File[] files = databasePath.toFile().listFiles();
            if (files == null || files.length == 0) {
                log(ModuleType.CORE, DatabaseLogger.LogLevel.ERROR, "No database files found!");
                writePool = new WritePool();
                return;
            }
            // convert the file to Database
            Set<DatabaseImpl> databases = Arrays.stream(files).map(file-> new DatabaseImpl(file.getName(), file)).collect(Collectors.toSet());
            writePool = new WritePool();
            databases.forEach(writePool::register);
            log(ModuleType.CORE, DatabaseLogger.LogLevel.INFO, "Write pool is created!");
        }
        public void initializeAllFiles() {
            createFileIfNotExists("config/settings.json");
            createFileIfNotExists("config/admins.json");
            createFileIfNotExists("config/security.json");
        }

        private void createFileIfNotExists(String path) {
            try {
                File file = new File(path);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                if (!file.exists()) {
                    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path);
                    if (inputStream == null) {
                        System.err.println("Error: " + path + " not found in resources!");
                        return;
                    }

                    Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
