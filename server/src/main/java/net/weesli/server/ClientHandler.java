package net.weesli.server;

import net.weesli.server.channel.ChannelSecurity;
import net.weesli.server.model.SocketResponse;
import net.weesli.server.channel.ChannelAuth;
import net.weesli.server.channel.ChannelReader;
import net.weesli.services.json.JsonBase;
import net.weesli.services.log.DatabaseLogger;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChannelReader reader;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        reader = new ChannelReader(socket);
        boolean isAllowedIp = ChannelSecurity.isAllowedIp(socket.getInetAddress().getHostAddress());
        if (!isAllowedIp) {
            stop();
            try {
                SocketResponse.error("Security Error: This ip is not allowed in this database.").send(socket);
            } catch (IOException e) {
                DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, e.getMessage());
            }
            return;
        }
        DatabaseLogger.logServer(DatabaseLogger.LogLevel.INFO, "Client connected" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
    }

    public void stop() {
        try {
            socket.close();
        } catch (IOException e) {
            DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, e.getMessage());
        }
        DatabaseLogger.logServer(DatabaseLogger.LogLevel.INFO, "Client disconnected" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
    }

    @Override
    public void run() {
        try (InputStream in = socket.getInputStream()) {
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                byte[] lengthBytes = in.readNBytes(4);
                if (lengthBytes.length < 4) {
                    stop();
                    break;
                }

                int length = ByteBuffer.wrap(lengthBytes).getInt();
                byte[] dataBytes = in.readNBytes(length);
                if (dataBytes.length < length) {
                    stop();
                    break;
                }
                JsonBase node = new JsonBase(dataBytes);

                if (!ChannelAuth.auth(node)) {
                    SocketResponse response = SocketResponse.error("Unauthorized: User is not authorized");
                    response.send(socket);
                    continue;
                }

                reader.read(node);
            }
        } catch (IOException exception) {
            DatabaseLogger.logServer(DatabaseLogger.LogLevel.ERROR, exception.getMessage());
        } finally {
            stop();
        }
    }

}
