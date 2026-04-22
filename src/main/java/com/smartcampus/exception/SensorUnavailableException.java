package com.smartcampus.exception;

/**
 * Thrown when an operation is attempted on a sensor in MAINTENANCE status.
 * The sensor is physically unavailable and must not accept new readings.
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}
