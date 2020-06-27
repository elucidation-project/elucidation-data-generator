package com.fortitudetec.elucidation.data.home.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fortitudetec.elucidation.data.home.db.DeviceDao;
import com.fortitudetec.elucidation.data.home.model.Device;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/home/device")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeviceResource {

    private final DeviceDao dao;

    public DeviceResource(DeviceDao dao) {
        this.dao = dao;
    }

    @GET
    @Timed
    @ExceptionMetered
    public Response listRegisteredDevices() {
        return Response.ok(dao.findAll()).build();
    }

    @POST
    @Path("/register")
    @Timed
    @ExceptionMetered
    public Response registerDevice(@NotNull Device device) {
        long id = dao.create(device);
        return Response.status(201).entity(Map.of("id", id)).build();
    }

    @DELETE
    @Path("/{id}")
    @Timed
    @ExceptionMetered
    public Response deleteDevice(@PathParam("id") long id) {
        dao.deleteDevice(id);
        return Response.accepted().build();
    }

}
