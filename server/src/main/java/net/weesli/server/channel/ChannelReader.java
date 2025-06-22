package net.weesli.server.channel;

import net.weesli.api.database.Collection;
import net.weesli.api.database.Database;
import net.weesli.server.Server;
import net.weesli.server.exception.AuthException;
import net.weesli.server.model.SocketResponse;
import net.weesli.services.json.JsonBase;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class ChannelReader {
    private final Socket socket;

    public ChannelReader(Socket socket) {
        this.socket = socket;
    }

    public void read(JsonBase node){
        String action = node.get("action").getAsString();
        SocketResponse response = null;
        try {
            switch (action) {
                case "connection" -> response =  handleConnection(node);
                case "insertorupdate" -> response =  handleInsertOrUpdate(node);
                case "findall" -> response = handleFindAll(node);
                case "find" -> response = handleFind(node);
                case "delete" -> response = handleDelete(node);
                case "findbyid" -> response = handleFindById(node);
                case "close" -> response = handleClose(node);
            }
        } catch (AuthException e) {
            response = SocketResponse.error(e.getMessage());
        }
        if (response != null){
            try {
                response.send(socket); // send response to client
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void assertPermission(JsonBase node, String permission) throws AuthException {
        if (!ChannelAuth.hasPermission(node, permission)){
            throw new AuthException("Unauthorized: missing permission '" + permission + "'");
        }
    }

    private SocketResponse handleClose(JsonBase node) throws AuthException {
        assertPermission(node, "write");
        Collection collection = getCollection(node);
        collection.close();
        return SocketResponse.success("CLOSED");
    }

    private SocketResponse handleInsertOrUpdate(JsonBase node) throws AuthException {
        assertPermission(node, "write");
        Collection collection = getCollection(node);
        JsonBase object = new JsonBase(node.get("object").getAsString().getBytes(StandardCharsets.UTF_8));
        String id = (object.has("id") ? object.get("id").getAsString() : null);
        String data = object.get("data").getAsString();
        byte[] response;
        if (id == null){
            response = collection.insertOrUpdate(data);
        }else {
            response = collection.insertOrUpdate(id, data);
        }
        String s = Base64.getEncoder().encodeToString(response);
        return SocketResponse.success(s);
    }

    private SocketResponse handleConnection(JsonBase node) throws AuthException {
        assertPermission(node, "read");
        Collection collection = getCollection(node); // this is register collection to database for load all data's
        return SocketResponse.success("CONNECTED");
    }

    private SocketResponse handleFindAll(JsonBase node) throws AuthException {
        assertPermission(node, "read");
        Collection collection = getCollection(node);
        List<String> result = collection.findAll().stream().map(e -> Base64.getEncoder().encodeToString(e)).toList();
        return SocketResponse.success(result.toString());
    }

    private SocketResponse handleFind(JsonBase node) throws AuthException {
        assertPermission(node, "read");
        Collection collection = getCollection(node);
        JsonBase object = new JsonBase(node.get("object").getAsString().getBytes(StandardCharsets.UTF_8));
        String where = object.get("where").getAsString();
        Object value = object.get("value").getAsString();
        List<String> result = collection.find(where, value).stream().map(e -> Base64.getEncoder().encodeToString(e)).toList();
        return SocketResponse.success(result.toString());
    }

    private SocketResponse handleDelete(JsonBase node) throws AuthException {
        assertPermission(node, "write");
        Collection collection = getCollection(node);
        JsonBase object = node.getAsJson("object");
        String id = object.get("id").getAsString();
        collection.delete(id);
        return SocketResponse.success(Base64.getEncoder().encodeToString("DELETED".getBytes(StandardCharsets.UTF_8)));
    }

    private SocketResponse handleFindById(JsonBase node) throws AuthException {
        assertPermission(node, "read");
        Collection collection = getCollection(node);
        JsonBase object = node.getAsJson("object");
        String id = object.get("id").getAsString();
        byte[] result = collection.findById(id);
        if (result == null){
            return SocketResponse.error("Not found");
        }
        return SocketResponse.success(Base64.getEncoder().encodeToString(result));
    }

    private Database getDatabase(JsonBase node){
        String dbName = node.get("database").getAsString();
        return Server.getProvider().getDatabases().stream().filter(database -> database.getName().equals(dbName)).findFirst().orElse(null);
    }

    private Collection getCollection(JsonBase node){
        Database database = getDatabase(node);
        if (database == null) {
            throw new RuntimeException("Database not found");
        }
        String collectionName = node.get("collection").getAsString();
        Collection collection = database.getCollection(collectionName);
        if (collection == null) {
            throw new RuntimeException("Collection not found");
        }
        return collection;
    }

}
