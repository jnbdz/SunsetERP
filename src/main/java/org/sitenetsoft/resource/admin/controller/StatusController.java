package org.sitenetsoft.resource.admin.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.sitenetsoft.framework.entity.GenericValue;
//import org.sitenetsoft.framework.common.status.StatusServices;

import java.util.List;

@Path("/admin/status")
public class StatusController {

    /*@Inject
    StatusServices statusServices;*/

    @GET
    @Path("/{statusTypeId}")
    public String getStatusItems(@PathParam("statusTypeId") String statusTypeId) {
        //List<GenericValue>
        //@TODO: Quarkus
        //return statusServices.getStatusItems(statusTypeId);
        return "Status";
    }

}
