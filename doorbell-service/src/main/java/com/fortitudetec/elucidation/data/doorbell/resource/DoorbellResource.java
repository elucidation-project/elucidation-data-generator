package com.fortitudetec.elucidation.data.doorbell.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fortitudetec.elucidation.data.doorbell.db.DoorbellDao;
import com.fortitudetec.elucidation.data.doorbell.model.Doorbell;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.Map;

@Path("/doorbell")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DoorbellResource {

    private DoorbellDao dao;

    public DoorbellResource(DoorbellDao dao) {
        this.dao = dao;
    }

    @GET
    @Timed
    @ExceptionMetered
    public Response listRegisteredDoorbells() {
        return Response.ok(dao.findAll()).build();
    }

    @GET
    @Path("/{id}/status")
    @Timed
    @ExceptionMetered
    public Response findDoorbell(@PathParam("id") long id) {
        var optionalDoorbell = dao.findById(id);
        return Response.ok(optionalDoorbell.orElseThrow(NotFoundException::new)).build();
    }

    @POST
    @Path("/register")
    @Timed
    @ExceptionMetered
    public Response registerDoorbell(@BeanParam Doorbell doorbell) {
        long id = dao.create(doorbell);

        var uri = UriBuilder.fromUri("doorbell/{id}/status").build(id);
        return Response.created(uri).entity(Map.of("id", id)).build();
    }

    @DELETE
    @Path("/{id}")
    @Timed
    @ExceptionMetered
    public Response deleteDoorbell(@PathParam("id") long id) {
        dao.deleteDoorbell(id);
        return Response.accepted().build();
    }

    @POST
    @Path("/{id}/ring")
    @Timed
    @ExceptionMetered
    public Response ringDoorbell(@PathParam("id") long id) {
        // TODO: Trigger doorbell ring
        return Response.accepted().build();
    }
}
