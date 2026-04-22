
package com.smartcampus;

import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.mapper.*;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Tell Jersey exactly which package to scan
        packages("com.smartcampus");

        // Also register explicitly to be safe
        register(DiscoveryResource.class);
        register(RoomResource.class);
        register(SensorResource.class);
        register(RoomNotEmptyExceptionMapper.class);
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(SensorUnavailableExceptionMapper.class);
        register(GlobalExceptionMapper.class);
        register(LoggingFilter.class);
        register(JacksonFeature.class);
    }
}
