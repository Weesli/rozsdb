package net.weesli.server.controller;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import net.weesli.api.database.Collection;
import net.weesli.api.database.Database;
import net.weesli.server.Server;
import net.weesli.services.ServiceFactory;
import net.weesli.services.user.UserService;

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
        router.get("/databases/:database/collections/:collection/findbyid/:id").handler(this::handleFindById);
        router.get("/databases/:database/collections/:collection/find/:field/:value").handler(this::handleFind);
        router.get("/databases/:database/collections/:collection/findall").handler(this::handleFindAll);
        router.get("/databases/:database/collections/:collection/delete/:id").handler(this::handleDelete);
        router.post("/databases/:database/collections/:collection/insertorupdate/:id").handler(this::handleInsertOrUpdate);
        router.post("/databases/:database/collections/:collection/insertorupdate/").handler(this::handleInsertOrUpdate);
    }

    private void authenticate(RoutingContext context) {
        String admin = context.request().headers().get("admin");
        if (admin == null || !userService.isAdmin(admin)) {
            sendErrorResponse(context, 403, "Unauthorized");
            return;
        }
        context.put("user", admin);
        context.next();
    }

    public void handleFindById(RoutingContext context) {
        if (!checkPermission(context, "read")) return;

        withDatabaseAndCollection(context, (database, collection) -> {
            String id = context.pathParam("id");
            String response = collection.findById(id);

            if (response == null) {
                sendErrorResponse(context, 404, "Data not found");
                return;
            }

            sendSuccessResponse(context, response);
        });
    }

    public void handleFind(RoutingContext context) {
        if (!checkPermission(context, "read")) return;

        withDatabaseAndCollection(context, (database, collection) -> {
            String field = context.request().getParam("field");
            String value = context.request().getParam("value");
            List<String> response = collection.find(field, value);

            if (response == null || response.isEmpty()) {
                sendErrorResponse(context, 404, "Data not found");
                return;
            }

            sendSuccessResponse(context, response.toString());
        });
    }

    public void handleFindAll(RoutingContext context) {
        if (!checkPermission(context, "read")) return;

        withDatabaseAndCollection(context, (database, collection) -> {
            String response = collection.findAll().toString();
            sendSuccessResponse(context, response);
        });
    }

    public void handleDelete(RoutingContext context) {
        if (!checkPermission(context, "write")) return;

        withDatabaseAndCollection(context, (database, collection) -> {
            String id = context.pathParam("id");
            collection.delete(id);
            sendSuccessResponse(context, "Data deleted successfully");
        });
    }

    public void handleInsertOrUpdate(RoutingContext context) {
        if (!checkPermission(context, "write")) return;

        withDatabaseAndCollection(context, (database, collection) -> {
            String id = context.pathParam("id");

            context.request().body().onSuccess(buffer -> {
                String jsonData = buffer.toString();

                if (id != null) {
                    collection.insertOrUpdate(id, jsonData);
                } else {
                    collection.insertOrUpdate(jsonData);
                }

                sendSuccessResponse(context, "Data inserted or updated successfully");
            }).onFailure(err -> {
                sendErrorResponse(context, 400, "Error reading body");
            });
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
        Optional<Collection> collectionOpt = database.getCollections().stream()
                .filter(c -> c.getCollectionName().equals(collectionName))
                .findFirst();

        if (collectionOpt.isEmpty()) {
            sendErrorResponse(context, 404, "Collection not found");
            return;
        }

        consumer.accept(database, collectionOpt.get());
    }

    private void sendSuccessResponse(RoutingContext context, String message) {
        context.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end("{ \"message\": \"" + message + "\" }");
    }

    private void sendErrorResponse(RoutingContext context, int statusCode, String message) {
        context.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end("{ \"message\": \"" + message + "\" }");
    }

    @FunctionalInterface
    private interface DatabaseCollectionConsumer {
        void accept(Database database, Collection collection);
    }
}