package org.sitenetsoft.sunseterp;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.*;

@Path("/sunseterp")
@Tag(name = "SunsetERP API", description = "Resources SunsetERP endpoints.")
public class SunsetErpResource {

    @ConfigProperty(name = "sunseterp.api.info.access.ips", defaultValue = "127.0.0.1")
    List<String> ipAddresses;

    private String profile() {
        return System.getProperty("quarkus.profile", "prod");
    }

    @GET
    @Path("/info")
    @Operation(
            summary = "Get system information",
            description = "Returns system properties if the user is authorized and the profile is 'dev'"
    )
    @APIResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    // When not logged in, the user is unauthorized
    @APIResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Error.class))
    )
    // When logged in, the user is forbidden to access the resource
    @APIResponse(
            responseCode = "403",
            description = "Forbidden",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Error.class))
    )
    public Response info() {
        String profile = profile();
        String allowDisplayInfo = System.getProperty("sunseterp.api.info.access", "false");
        // && !"dev".equals(profile)
        /*if (!Boolean.parseBoolean(allowDisplayInfo)) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Error("Access to the resource is forbidden")).build();
        }*/
        return Response.ok(System.getProperties()).build();
    }

    @HEAD
    @Path("/health")
    @Operation(
            summary = "Check system health",
            description = "Returns the health status of the system if the user is authorized and the profile is 'dev'"
    )
    @APIResponse(
            responseCode = "200",
            description = "OK"
    )
    // When not logged in, the user is unauthorized
    @APIResponse(
            responseCode = "401",
            description = "Unauthorized"
    )
    // When logged in, the user is forbidden to access the resource
    @APIResponse(
            responseCode = "403",
            description = "Forbidden",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Error.class))
    )
    public Response health() {
        String profile = profile();
        String allowDisplayInfo = System.getProperty("sunseterp.api.health.access", "false");
        //&& !"dev".equals(profile)
        /*if (!Boolean.parseBoolean(allowDisplayInfo)) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Error("Access to the resource is forbidden")).build();
        }*/
        return Response.noContent().header("Health-Status", "OK").build();
    }

    @GET
    @Path("/version")
    @Operation(
            summary = "Check system health",
            description = "Returns the health status of the system if the user is authorized and the profile is 'dev'"
    )
    @APIResponse(
            responseCode = "200",
            description = "OK"
    )
    // When not logged in, the user is unauthorized
    @APIResponse(
            responseCode = "401",
            description = "Unauthorized"
    )
    // When logged in, the user is forbidden to access the resource
    @APIResponse(
            responseCode = "403",
            description = "Forbidden",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Error.class))
    )
    public Response version() {
        String profile = profile();
        String allowDisplayInfo = System.getProperty("sunseterp.api.version.access", "false");
        /*if (!Boolean.parseBoolean(allowDisplayInfo) && !"dev".equals(profile)) {
            return Response.status(Response.Status.FORBIDDEN).entity(new Error("Access to the resource is forbidden")).build();
        }*/
        return Response.ok("SunsetERP 1.0").build();
    }

    // TODO: Status endpoint for SunsetERP, and the resources it is connected to like the database, OpenSearch, ElasticSearch, etc.
    // TODO: Add access to OpenAPI documentation
    // TODO: Add access to the Swagger UI
    // TODO: Add access to the json-schema documentation
}
