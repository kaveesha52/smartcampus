
package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/discover")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("api",     "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("status",  "operational");
        response.put("contact", Map.of(
                "team",  "Westminster Campus Infrastructure Team",
                "email", "smartcampus@westminster.ac.uk"
        ));

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    "/api/v1/discover");
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("_links", links);

        Map<String, Object> resources = new LinkedHashMap<>();
        resources.put("rooms",   Map.of(
                "href",        "/api/v1/rooms",
                "description", "Manage campus rooms and their decommissioning",
                "methods",     new String[]{"GET", "POST", "DELETE"}
        ));
        resources.put("sensors", Map.of(
                "href",        "/api/v1/sensors",
                "description", "Register, query and filter sensors by type",
                "methods",     new String[]{"GET", "POST"}
        ));
        response.put("resources", resources);

        return Response.ok(response).build();
    }

    @GET
    @Path("/error")
    public Response triggerError() {
        String x = null;
        x.length(); // 🔥 forces NullPointerException
        return Response.ok().build();
    }
}