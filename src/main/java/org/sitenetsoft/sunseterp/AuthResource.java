package org.sitenetsoft.sunseterp;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class AuthResource {}

/*@Path("/auth")
public class AuthResource {

    private final Set<String> authInfos = Collections.synchronizedSet(new LinkedHashSet<>());

    @POST
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
    public Response login(String username, String password) {
        return Response.ok(authInfos).build();
    }

    @POST
    @APIResponse(
            responseCode = "400",
            description = "Invalid User ID",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "503",
            description = "Service Unavailable",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
    @APIResponse(
            responseCode = "504",
            description = "Gateway Timeout",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
    )
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
    public Response logout(String userId) {

        return Response.ok(authInfos).build();
    }

}*/
