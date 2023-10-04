package org.sitenetsoft.resource.service.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sitenetsoft.framework.service.DispatchContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Path("/service")
public class ServiceController {

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServiceList() {
        DispatchContext dispatchContext = new DispatchContext("main", this.getClass().getClassLoader(), null);
        Set<String> serviceNames = dispatchContext.getAllServiceNames();

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("message", "OFBiz Services.");
        responseMap.put("services", serviceNames);

        System.out.println(System.getProperty("java.class.path"));

        return Response.ok(responseMap).build();
    }

    @GET
    @Path("/details")
    public String getDetails() {
        return System.getProperty("java.class.path");
    }

}
