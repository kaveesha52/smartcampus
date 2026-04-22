
package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.net.URI;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {


        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create("http://0.0.0.0:8080/api/v1/"),
                new SmartCampusApplication());

        LOGGER.info("Smart Campus API started.");
        LOGGER.info("Endpoints available at: http://localhost:8080/api/v1");
        LOGGER.info("Press CTRL+C to stop.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down...");
            server.shutdownNow();
        }));

        Thread.currentThread().join();
    }
}