package net.weesli.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import lombok.Getter;
import lombok.SneakyThrows;
import net.weesli.api.DatabasePoolProvider;
import net.weesli.api.DatabasePool;
import net.weesli.server.controller.CollectionController;
import net.weesli.services.log.DatabaseLogger;

@Getter
public class Server extends AbstractVerticle {

    private final DatabasePool databasePool;
    private final int port;

    @SneakyThrows
    public Server(DatabasePoolProvider provider, int port) {
        this.databasePool = provider.getDatabasePool();
        this.port = port;
        start();
    }

    @Override
    public void start() {
        initializeServer();
    }

    private void initializeServer() {
        vertx = Vertx.vertx();
        Router router = Router.router(vertx);
        addRouters(router);
        startServer(router);
    }

    private void startServer(Router router) {
        vertx.createHttpServer().requestHandler(router).listen(port, result -> {
            if (result.succeeded()) {
                DatabaseLogger.log(DatabaseLogger.ModuleType.SERVER, DatabaseLogger.LogLevel.INFO, String.format("Server is running on port %s", port));
            } else {
                DatabaseLogger.log(DatabaseLogger.ModuleType.SERVER, DatabaseLogger.LogLevel.ERROR, "Failed to start server: " + result.cause().getMessage());
            }
        });
    }

    private void addRouters(Router router) {
        new CollectionController(this).addRoutes(router);
    }
}
