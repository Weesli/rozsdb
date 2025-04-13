package net.weesli.server.controller;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;
import net.weesli.api.database.Collection;
import net.weesli.api.database.Database;
import net.weesli.server.ResponseMessage;
import net.weesli.server.Server;
import net.weesli.services.ServiceFactory;
import net.weesli.services.mapper.ObjectMapperProvider;
import net.weesli.services.security.SecurityService;
import net.weesli.services.user.UserService;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class CollectionController {

    private final Server server;
    private final UserService userService;

    public CollectionController(Server server) {
        this.server = server;
        this.userService = (UserService) ServiceFactory.getService(UserService.class);
    }

    public void addRoutes(Router router) {
        router.route("/databases/:database/collections/*").handler(this::authenticate);
        router.get("/databases/:database/collections/:collection").handler(this::handleInitCollection);
        router.post("/databases/:database/collections/:collection/findbyid").handler(this::handleFindById);
        router.post("/databases/:database/collections/:collection/find").handler(this::handleFind);
        router.post("/databases/:database/collections/:collection/findall").handler(this::handleFindAll);
        router.post("/databases/:database/collections/:collection/delete").handler(this::handleDelete);
        router.post("/databases/:database/collections/:collection/insertorupdate").handler(this::handleInsertOrUpdate);
    }

    private void authenticate(RoutingContext context) {
        String ip = context.request().remoteAddress().host();
        if (!getSecurityService().getIpChecker().checkIp(ip)) {
            sendErrorResponse(context, 403, "Unauthorized");
            return;
        }
        String admin = context.request().headers().get("admin");
        if (admin == null || !userService.isAdmin(admin)) {
            sendErrorResponse(context, 403, "Unauthorized");
            return;
        }
        context.put("user", admin);
        context.next();
    }

    private void handleInitCollection(RoutingContext context) { // this endpoint created for pre-warn collection
        if (!checkPermission(context, "read")) return;

        withDatabaseAndCollection(context, (database, collection) -> {
            sendSuccessResponse(context, "");
        });
    }

    public void handleFindById(RoutingContext context) {
        if (!checkPermission(context, "read")) return;

        context.request().body().onSuccess(buffer -> {
            JsonObject body = new JsonObject(buffer.toString());
            String id = body.getString("id");

            if (id == null) {
                sendErrorResponse(context, 400, "Missing id parameter");
                return;
            }

            withDatabaseAndCollection(context, (database, collection) -> {
                byte[] response = collection.findById(id);

                if (response == null) {
                    sendErrorResponse(context, 404, "Data not found");
                    return;
                }
                String encodedResponse = Base64.getEncoder().encodeToString(response);
                sendSuccessResponse(context, encodedResponse);
            });
        }).onFailure(err -> {
            sendErrorResponse(context, 400, "Error reading body");
        });
    }

    public void handleFind(RoutingContext context) {
        if (!checkPermission(context, "read")) return;

        context.request().body().onSuccess(buffer -> {
            JsonObject body = new JsonObject(buffer.toString());
            String field = body.getString("field");
            String value = body.getString("value");

            if (field == null || value == null) {
                sendErrorResponse(context, 400, "Missing field or value parameters");
                return;
            }

            withDatabaseAndCollection(context, (database, collection) -> {
                List<byte[]> response = collection.find(field, value);

                if (response == null || response.isEmpty()) {
                    sendErrorResponse(context, 404, "Data not found");
                    return;
                }
                List<String> encodedResponse = response.stream()
                        .map(data -> Base64.getEncoder().encodeToString(data))
                        .toList();
                sendSuccessResponse(context, encodedResponse.toString());
            });
        }).onFailure(err -> {
            sendErrorResponse(context, 400, "Error reading body");
        });
    }

    public void handleFindAll(RoutingContext context) {
        if (!checkPermission(context, "read")) return;

        withDatabaseAndCollection(context, (database, collection) -> {
            List<byte[]> response = collection.findAll();
            if (response == null || response.isEmpty()) {
                sendErrorResponse(context, 404, "No data found");
                return;
            }
            List<String> encodedResponse = response.stream()
                    .map(data -> Base64.getEncoder().encodeToString(data))
                    .toList();
            sendSuccessResponse(context, encodedResponse.toString());
        });
    }

    public void handleDelete(RoutingContext context) {
        if (!checkPermission(context, "write")) return;

        context.request().body().onSuccess(buffer -> {
            JsonObject body = new JsonObject(buffer.toString());
            String id = body.getString("id");

            if (id == null) {
                sendErrorResponse(context, 400, "Missing id parameter");
                return;
            }

            withDatabaseAndCollection(context, (database, collection) -> {
                boolean status = collection.delete(id);
                if (!status) {
                    sendErrorResponse(context, 404, "Data not found");
                    return;
                }
                sendSuccessResponse(context, "Data deleted successfully");
            });
        }).onFailure(err -> {
            sendErrorResponse(context, 400, "Error reading body");
        });
    }

    public void handleInsertOrUpdate(RoutingContext context) {
        if (!checkPermission(context, "write")) return;

        context.request().body().onSuccess(buffer -> {
            JsonObject body = new JsonObject(buffer.toString());
            String id = body.getString("id");
            String jsonData = body.getString("data");

            if (jsonData == null) {
                sendErrorResponse(context, 400, "Missing data parameter");
                return;
            }

            withDatabaseAndCollection(context, (database, collection) -> {
                byte[] data;
                if (id != null) {
                    data = collection.insertOrUpdate(id, jsonData);
                } else {
                    data = collection.insertOrUpdate(jsonData);
                }
                String encodedData = Base64.getEncoder().encodeToString(data);
                sendSuccessResponse(context, encodedData);
            });
        }).onFailure(err -> {
            sendErrorResponse(context, 400, "Error reading body");
        });
    }

    private boolean checkPermission(RoutingContext context, String permission) {
        String user = context.get("user").toString();
        if (!userService.hasPermission(user, permission)) {
            sendErrorResponse(context, 403, "Unauthorized");
            return false;
        }
        return true;
    }

    private void withDatabaseAndCollection(RoutingContext context, DatabaseCollectionConsumer consumer) {
        String databaseName = context.pathParam("database");
        Optional<Database> databaseOpt = server.getDatabasePool().getDatabases().stream()
                .filter(d -> d.getName().equals(databaseName))
                .findFirst();

        if (databaseOpt.isEmpty()) {
            sendErrorResponse(context, 404, "Database not found");
            return;
        }

        Database database = databaseOpt.get();
        String collectionName = context.pathParam("collection");
        Collection collection = database.getCollection(collectionName);

        if (collection == null) {
            sendErrorResponse(context, 404, "Collection not found");
            return;
        }

        if (collection.isTimeout()){
            sendErrorResponse(context, 408, "Collection timeout");
            return;
        }

        consumer.accept(database, collection);
    }

    private void sendSuccessResponse(RoutingContext context, String message) {
        context.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(ResponseMessage.success(message).toString());
    }

    private void sendErrorResponse(RoutingContext context, int statusCode, String message) {
        context.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(ResponseMessage.error(message).toString());
    }

    private SecurityService getSecurityService() {
        return (SecurityService) ServiceFactory.getService(SecurityService.class);
    }

    @FunctionalInterface
    private interface DatabaseCollectionConsumer {
        void accept(Database database, Collection collection);
    }
}