//package com.smartcampus;
//
//import org.glassfish.grizzly.http.server.HttpServer;
//import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
//import org.glassfish.jersey.server.ResourceConfig;
//
//import java.net.URI;
//import java.util.logging.Logger;
//
///**
// * Main entry point. Bootstraps the Grizzly embedded HTTP server.
// * Run with: java -jar smart-campus-api-1.0.0.jar
// * API available at: http://localhost:8080/api/v1
// */
//public class Main {
//
//    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
//    public static final String BASE_URI = "http://0.0.0.0:8080/";
//
//    public static void main(String[] args) throws Exception {
//        final ResourceConfig config = new SmartCampusApplication();
//        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
//                URI.create(BASE_URI), config);
//
//        LOGGER.info("Smart Campus API started.");
//        LOGGER.info("Endpoints available at: http://localhost:8080/api/v1");
//        LOGGER.info("Press CTRL+C to stop.");
//
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            LOGGER.info("Shutting down Smart Campus API...");
//            server.shutdownNow();
//        }));
//
//        Thread.currentThread().join();
//    }
//}
package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.net.URI;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {

        // Include /api/v1 in the base URI directly here
        // and REMOVE @ApplicationPath from SmartCampusApplication
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