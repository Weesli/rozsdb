package net.weesli.server.model;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public record SocketResponse(boolean success, String message) {

    public static SocketResponse success(String message) {
        return new SocketResponse(true, message);
    }

    public static SocketResponse error(String message) {
        return new SocketResponse(false, message);
    }

    public String getJson(){
        return "{\"status\":\"" + (success ? "success" : "error") + "\", \"message\":\"" + message + "\"}";
    }

    public void send(Socket socket) throws IOException {
        OutputStream out = socket.getOutputStream();
        byte[] jsonBytes = getJson().getBytes(StandardCharsets.UTF_8);
        out.write(ByteBuffer.allocate(4).putInt(jsonBytes.length).array());
        out.write(jsonBytes);
        out.flush();
    }

}
