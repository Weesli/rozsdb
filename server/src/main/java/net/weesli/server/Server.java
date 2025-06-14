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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {

    private ServerSocket server;
    private final ExecutorService clientPool;
    private volatile boolean running = false;
    private final int port;

    @Getter
    @Setter
    private static DatabaseProvider provider;

    public Server(DatabaseProvider provider) {
        int maxClientCount = provider.getCoreSettings().getSettings().get("maxClientCount", Integer.class);
        this.port = provider.getCoreSettings().getSettings().get("port", Integer.class);

        clientPool = Executors.newFixedThreadPool(maxClientCount);
        Server.provider = provider;

        try {
            server = new ServerSocket(port);
            DatabaseLogger.logServer(DatabaseLogger.LogLevel.INFO, "Server started on port " + port);
        } catch (IOException e) {
            DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, "Failed to start server on port " + port + ": " + e.getMessage());
            throw new RuntimeException("Server could not be started", e);
        }
    }

    public void start() {
        if (running) {
                DatabaseLogger.logServer(DatabaseLogger.LogLevel.WARN, "Server is already running");
            return;
        }

        running = true;
        DatabaseLogger.logServer(DatabaseLogger.LogLevel.INFO, "Server is now accepting connections on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        while (running) {
            Socket socket = acceptClient();
            if (socket != null && running) {
                try {
                    clientPool.execute(new ClientHandler(socket));
                } catch (Exception e) {
                    DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, "Error handling client: " + e.getMessage());
                    try {
                        socket.close();
                    } catch (IOException ioException) {
                        DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, "Error closing socket: " + ioException.getMessage());
                    }
                }
            }
        }
    }

    private Socket acceptClient() {
        try {
            if (server == null || server.isClosed()) {
                return null;
            }

            Socket socket = server.accept();
            DatabaseLogger.logServer(DatabaseLogger.LogLevel.INFO, "New connection accepted from: " +
                    socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
            return socket;

        } catch (IOException e) {
            if (running) {
                DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, "Error accepting client connection: " + e.getMessage());
            }
            return null;
        }
    }

    public void shutdown() {
        if (!running) {
            return;
        }

        DatabaseLogger.logServer(DatabaseLogger.LogLevel.INFO, "Shutting down server...");
        running = false;

        try {
            if (server != null && !server.isClosed()) {
                server.close();
                DatabaseLogger.logServer(DatabaseLogger.LogLevel.INFO, "Server socket closed");
            }

            clientPool.shutdown();

            if (!clientPool.awaitTermination(5, TimeUnit.SECONDS)) {
                DatabaseLogger.logServer(DatabaseLogger.LogLevel.WARN, "Thread pool did not terminate gracefully, forcing shutdown");
                clientPool.shutdownNow();

                if (!clientPool.awaitTermination(2, TimeUnit.SECONDS)) {
                    DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, "Thread pool did not terminate after forced shutdown");
                }
            }

            DatabaseLogger.logServer(DatabaseLogger.LogLevel.INFO, "Server shutdown completed");

        } catch (IOException e) {
            DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, "Error during server shutdown: " + e.getMessage());
        } catch (InterruptedException e) {
            DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, "Server shutdown interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public int getActiveClientCount() {
        return ((ThreadPoolExecutor) clientPool).getActiveCount();
    }
}