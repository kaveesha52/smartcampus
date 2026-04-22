package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sub-Resource for sensor reading history.
 *
 * Accessed via the Sub-Resource Locator in SensorResource:
 *   GET  /api/v1/sensors/{sensorId}/readings   → reading history
 *   POST /api/v1/sensors/{sensorId}/readings   → append new reading
 *
 * Note: This class has NO @Path annotation at the class level — it is
 * instantiated by SensorResource's locator, which already resolved the path.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns the full reading history for this sensor.
     */
    @GET
    public Response getReadings() {
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

        List<SensorReading> history = DataStore.readings
                .getOrDefault(sensorId, new CopyOnWriteArrayList<>());

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("sensorId", sensorId);
        responseBody.put("count", history.size());
        responseBody.put("readings", history);
        responseBody.put("_links", Map.of(
                "sensor", "/api/v1/sensors/" + sensorId
        ));

        return Response.ok(responseBody).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     *
     * Appends a new reading. Throws SensorUnavailableException (→ 403) if the
     * sensor is in MAINTENANCE state — it cannot accept data whilst offline.
     *
     * Side Effect: updates the sensor's currentValue to the new reading's value
     * to keep the parent entity consistent with the latest data.
     */
    @POST
    public Response addReading(SensorReading reading) {
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

        // State constraint: sensors in MAINTENANCE cannot receive readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently in MAINTENANCE mode "
                    + "and cannot accept new readings. Update sensor status to ACTIVE first."
            );
        }

        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Reading body is required."))
                    .build();
        }

        // Generate ID and timestamp if not provided
        SensorReading newReading = new SensorReading(reading.getValue());
        if (reading.getId() != null) newReading.setId(reading.getId());

        // Persist the reading
        DataStore.readings.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>())
                .add(newReading);

        // Side effect: update parent sensor's currentValue for data consistency
        sensor.setCurrentValue(newReading.getValue());

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("message",  "Reading recorded successfully.");
        responseBody.put("reading",  newReading);
        responseBody.put("sensorCurrentValue", sensor.getCurrentValue());
        responseBody.put("_links", Map.of(
                "history", "/api/v1/sensors/" + sensorId + "/readings",
                "sensor",  "/api/v1/sensors/" + sensorId
        ));

        return Response.status(Response.Status.CREATED).entity(responseBody).build();
    }
}
