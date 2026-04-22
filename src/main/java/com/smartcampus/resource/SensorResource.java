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


@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {


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


    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
