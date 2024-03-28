package org.sitenetsoft.sunseterp;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.*;

@Path("/sunseterp")
public class SunsetErpResource {

    private final Map<String, String> properties = new HashMap<>();

    @GET
    @Path("/info")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid Username or Password",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    public Response info() {
        Locale currentLocale = Locale.getDefault();
        TimeZone currentTimeZone = TimeZone.getDefault();

        properties.put("ofbiz.home", System.getProperty("ofbiz.home"));
        properties.put("ofbiz.admin.host", System.getProperty("ofbiz.admin.host"));
        properties.put("ofbiz.admin.key", System.getProperty("ofbiz.admin.key"));
        properties.put("ofbiz.admin.port", System.getProperty("ofbiz.admin.port"));
        properties.put("ofbiz.start.loaders", System.getProperty("ofbiz.start.loaders"));
        properties.put("ofbiz.log.dir", System.getProperty("ofbiz.log.dir"));
        properties.put("ofbiz.auto.shutdown", System.getProperty("ofbiz.auto.shutdown"));
        properties.put("ofbiz.enable.hook", System.getProperty("ofbiz.enable.hook"));
        properties.put("java.awt.headless", System.getProperty("java.awt.headless"));
        properties.put("derby.system.home", System.getProperty("derby.system.home"));
        properties.put("defaultLocale", currentLocale.toString());
        properties.put("defaultTimeZone", currentTimeZone.getID());

        return Response.ok(properties).build();
    }

    @GET
    @Path("/status")
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid Username or Password",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    public Response status() {
        return Response.noContent().header("X-Status", "OK").build();
    }

}
