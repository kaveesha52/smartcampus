package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralised in-memory data store.
 *
 * Uses ConcurrentHashMap for all collections to ensure thread-safety
 * when multiple requests access/modify data concurrently.
 *
 * This is a singleton — the single source of truth for the entire API.
 * Pre-loaded with sample data so the API is demo-ready on startup.
 */
public class DataStore {

    // Thread-safe maps: key = entity ID, value = entity object
    public static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();

    // Sensor readings keyed by sensorId → list of readings for that sensor
    public static final ConcurrentHashMap<String, CopyOnWriteArrayList<SensorReading>> readings =
            new ConcurrentHashMap<>();

    // Seed with realistic sample data so video demo works immediately
    static {
        // Rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-102", "Computer Science Lab", 30);
        Room r3 = new Room("AUD-001", "Main Auditorium", 400);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        rooms.put(r3.getId(), r3);

        // Sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001",  "CO2",         "ACTIVE", 412.0, "LIB-301");
        Sensor s3 = new Sensor("OCC-001",  "Occupancy",   "MAINTENANCE", 0.0, "LAB-102");
        Sensor s4 = new Sensor("TEMP-002", "Temperature", "ACTIVE", 19.8, "AUD-001");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);
        sensors.put(s4.getId(), s4);

        // Link sensors to rooms
        r1.getSensorIds().add("TEMP-001");
        r1.getSensorIds().add("CO2-001");
        r2.getSensorIds().add("OCC-001");
        r3.getSensorIds().add("TEMP-002");

        // Seed some readings
        CopyOnWriteArrayList<SensorReading> r1Readings = new CopyOnWriteArrayList<>();
        r1Readings.add(new SensorReading(21.0));
        r1Readings.add(new SensorReading(21.5));
        readings.put("TEMP-001", r1Readings);

        readings.put("CO2-001",  new CopyOnWriteArrayList<>(List.of(new SensorReading(410.0))));
        readings.put("OCC-001",  new CopyOnWriteArrayList<>());
        readings.put("TEMP-002", new CopyOnWriteArrayList<>(List.of(new SensorReading(19.8))));
    }
}
