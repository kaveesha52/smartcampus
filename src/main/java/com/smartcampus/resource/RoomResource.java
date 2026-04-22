package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * Room Resource — manages /api/v1/rooms
 *
 * GET    /api/v1/rooms          → list all rooms
 * POST   /api/v1/rooms          → create a new room
 * GET    /api/v1/rooms/{roomId} → fetch room details
 * DELETE /api/v1/rooms/{roomId} → decommission room (blocked if sensors present)
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    /**
     * GET /api/v1/rooms
     * Returns the full list of all rooms including their sensor IDs.
     */
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(DataStore.rooms.values());
        return Response.ok(roomList).build();
    }

    /**
     * POST /api/v1/rooms
     * Creates a new room. Returns 201 Created with the created resource.
     */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Room ID is required"))
                    .build();
        }

        if (DataStore.rooms.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of(
                            "status",  409,
                            "error",   "Conflict",
                            "message", "A room with ID '" + room.getId() + "' already exists."
                    ))
                    .build();
        }

        // Ensure sensorIds list is initialised
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        DataStore.rooms.put(room.getId(), room);

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("message", "Room created successfully.");
        responseBody.put("room", room);
        responseBody.put("_links", Map.of(
                "self",    "/api/v1/rooms/" + room.getId(),
                "sensors", "/api/v1/sensors"
        ));

        return Response.status(Response.Status.CREATED).entity(responseBody).build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Fetches full metadata for a specific room.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "status",  404,
                            "error",   "Not Found",
                            "message", "No room found with ID: " + roomId
                    ))
                    .build();
        }
        return Response.ok(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     *
     * Decommissions a room. Blocked if any sensors are still assigned to it.
     * This prevents data orphans (sensors with no parent room).
     *
     * Idempotency: The first DELETE removes the room (204). Subsequent
     * DELETE requests on the same ID return 404 because the room no longer
     * exists — technically not "idempotent" in the pure sense but safe: the
     * server state after each call is identical (room is gone). Widely
     * accepted as idempotent in REST practice.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.rooms.get(roomId);

        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "status",  404,
                            "error",   "Not Found",
                            "message", "No room found with ID: " + roomId
                    ))
                    .build();
        }

        // Safety check: block deletion if active sensors are still assigned
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Cannot decommission room '" + roomId + "'. It still has "
                    + room.getSensorIds().size() + " active sensor(s) assigned: "
                    + room.getSensorIds() + ". Reassign or remove all sensors first."
            );
        }

        DataStore.rooms.remove(roomId);
        return Response.noContent().build(); // 204 No Content
    }
}
