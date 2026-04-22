package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Sensor Resource — manages /api/v1/sensors
 *
 * GET  /api/v1/sensors              → list all sensors (optional ?type= filter)
 * POST /api/v1/sensors              → register a new sensor (validates roomId exists)
 * GET  /api/v1/sensors/{sensorId}   → fetch a specific sensor
 * *    /api/v1/sensors/{sensorId}/readings → delegated to SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    /**
     * GET /api/v1/sensors
     * GET /api/v1/sensors?type=CO2
     *
     * Returns all sensors. Optional query parameter 'type' filters by sensor type
     * (case-insensitive match).
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(DataStore.sensors.values());

        if (type != null && !type.isBlank()) {
            result = result.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        return Response.ok(result).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     * Fetches a specific sensor by ID.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "status",  404,
                            "error",   "Not Found",
                            "message", "No sensor found with ID: " + sensorId
                    ))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * POST /api/v1/sensors
     *
     * Registers a new sensor. The roomId field is validated — if the referenced
     * room does not exist, a LinkedResourceNotFoundException is thrown, which
     * maps to HTTP 422 Unprocessable Entity.
     *
     * @Consumes(APPLICATION_JSON): if client sends text/plain or application/xml,
     * JAX-RS automatically returns 415 Unsupported Media Type before this method
     * is even invoked.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Sensor ID is required."))
                    .build();
        }

        // Validate that the referenced room actually exists
        if (sensor.getRoomId() == null || !DataStore.rooms.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: roomId '" + sensor.getRoomId()
                    + "' does not exist. Create the room first."
            );
        }

        if (DataStore.sensors.containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of(
                            "status",  409,
                            "error",   "Conflict",
                            "message", "Sensor with ID '" + sensor.getId() + "' already exists."
                    ))
                    .build();
        }

        // Default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        DataStore.sensors.put(sensor.getId(), sensor);

        // Register sensor ID with its parent room
        DataStore.rooms.get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        // Initialise empty readings list for this sensor
        DataStore.readings.put(sensor.getId(), new CopyOnWriteArrayList<>());

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("message", "Sensor registered successfully.");
        responseBody.put("sensor", sensor);
        responseBody.put("_links", Map.of(
                "self",     "/api/v1/sensors/" + sensor.getId(),
                "readings", "/api/v1/sensors/" + sensor.getId() + "/readings",
                "room",     "/api/v1/rooms/" + sensor.getRoomId()
        ));

        return Response.status(Response.Status.CREATED).entity(responseBody).build();
    }

    /**
     * Sub-Resource Locator: /api/v1/sensors/{sensorId}/readings
     *
     * Rather than defining all nested reading paths here, we delegate to a
     * dedicated SensorReadingResource class. JAX-RS resolves this dynamically
     * at runtime — the locator returns an uninitialised resource instance and
     * JAX-RS injects the path parameters and dispatches the sub-request to it.
     *
     * This is the Sub-Resource Locator pattern. No HTTP verb annotation here —
     * JAX-RS recognises a method with only @Path as a locator, not an endpoint.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
