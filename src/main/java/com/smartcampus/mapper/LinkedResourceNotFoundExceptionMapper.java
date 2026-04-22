package com.smartcampus.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.ApiError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps LinkedResourceNotFoundException → HTTP 422 Unprocessable Entity.
 *
 * Why 422 over 404?
 * - 404 means the REQUEST URI was not found on the server.
 * - 422 means the server understood the request (URI valid, JSON well-formed)
 *   but the semantic content is invalid: a referenced resource (roomId) inside
 *   the payload does not exist. The issue is a broken internal reference, not
 *   a missing endpoint. 422 communicates this precisely.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        ApiError error = new ApiError(
                422,
                "Unprocessable Entity",
                exception.getMessage()
        );
        // 422 is not in javax.ws.rs.core.Response.Status enum for older JAX-RS
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
