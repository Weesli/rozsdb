package net.weesli.server;

import lombok.Getter;
import lombok.Setter;
import net.weesli.api.DatabaseProvider;
import net.weesli.services.log.DatabaseLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private ServerSocket server;
    private final ExecutorService clientPool;

    @Getter
    @Setter
    private static DatabaseProvider provider;

    public Server(DatabaseProvider provider) {
        int maxClientCount = provider.getCoreSettings().get("maxClientCount").asInt();
        int port = provider.getCoreSettings().get("port").asInt();
        clientPool = Executors.newFixedThreadPool(maxClientCount);
        Server.provider = provider;
        try {
            server = new ServerSocket(port);
            DatabaseLogger.logServer(DatabaseLogger.LogLevel.INFO, "Server started on port " + port);
            while (true) {
                Socket socket = acceptClient();
                if (socket != null) {
                    clientPool.execute(new ClientHandler(socket));
                }
            }
        } catch (IOException e) {
            DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, e.getMessage());
        } finally {
            shutdown();
        }
    }

    private Socket acceptClient() {
        try {
            return server.accept();
        } catch (IOException e) {
            DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, e.getMessage());
            return null;
        }
    }

    private void shutdown() {
        try {
            if (server != null && !server.isClosed()) {
                server.close();
            }
            clientPool.shutdown();
        } catch (IOException e) {
            DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, e.getMessage());
        }
    }
}
