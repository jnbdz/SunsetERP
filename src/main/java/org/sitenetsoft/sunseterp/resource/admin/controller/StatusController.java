package org.sitenetsoft.sunseterp.resource.admin.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
//import org.sitenetsoft.sunseterp.framework.common.status.StatusServices;


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
