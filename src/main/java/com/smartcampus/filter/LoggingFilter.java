package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * API observability filter — logs every inbound request and outbound response.
 *
 * Implements BOTH ContainerRequestFilter and ContainerResponseFilter in a
 * single class so one registration handles both directions.
 *
 * Uses java.util.logging.Logger (JUL) as required by the specification.
 *
 * Why filters over manual Logger.info() in every resource method?
 *   - Cross-cutting concern: logging applies to ALL endpoints. Filters enforce
 *     this universally without touching resource code.
 *   - DRY principle: zero duplication. Adding a new resource automatically
 *     gets logging for free.
 *   - Separation of concerns: business logic stays clean; infrastructure
 *     concerns (logging, auth, CORS) live in filters.
 *   - Consistency: every request/response is logged in exactly the same
 *     format regardless of which developer wrote the resource method.
 *   - Maintainability: changing the log format means editing one class, not
 *     every resource method across the entire codebase.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Intercepts every INCOMING request before it reaches a resource method.
     * Logs: HTTP method + full request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String uri    = requestContext.getUriInfo().getRequestUri().toString();

        LOGGER.info(String.format("[REQUEST]  --> %s %s", method, uri));
    }

    /**
     * Intercepts every OUTGOING response after the resource method returns.
     * Logs: HTTP method + URI + final status code.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        String method     = requestContext.getMethod();
        String uri        = requestContext.getUriInfo().getRequestUri().toString();
        int    statusCode = responseContext.getStatus();

        LOGGER.info(String.format("[RESPONSE] <-- %s %s | Status: %d", method, uri, statusCode));
    }
}
