package com.smartcampus.exception;

/**
 * Thrown when a client references a resource that does not exist inside a
 * valid JSON payload — e.g. posting a sensor with a non-existent roomId.
 *
 * This is semantically different from a 404: the request URI is valid and
 * the JSON is well-formed, but an internal foreign-key reference is broken.
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
