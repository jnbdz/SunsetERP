package org.sitenetsoft.sunseterp.resource.auth.controller;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sitenetsoft.sunseterp.resource.auth.model.User;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    @POST
    @Path("/login")
    @PermitAll
    public Response login(User user) {
        // In reality, Quarkus should handle authentication based on the Elytron properties file
        return Response.ok().entity("Login endpoint. Implement as needed.").build();
    }

    @POST
    @Path("/logout")
    @RolesAllowed("user")
    public Response logout() {
        return Response.ok().entity("Logout endpoint. Implement as needed.").build();
    }

    @POST
    @Path("/subscribe")
    @PermitAll
    public Response subscribe(User user) {
        // This is where you'd normally add the user to your properties file or database.
        // The properties file is static, so dynamic registration would require another form of storage.
        return Response.ok().entity("Subscribe endpoint. Implement as needed.").build();
    }
}
