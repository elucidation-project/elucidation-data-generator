package com.fortitudetec.elucidation.data.light.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fortitudetec.elucidation.data.light.db.SmartLightDao;
import com.fortitudetec.elucidation.data.light.model.SmartLight;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.Map;

@Path("/light")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SmartLightResource {

    private SmartLightDao dao;

    public SmartLightResource(SmartLightDao dao) {
        this.dao = dao;
    }

    @GET
    @Timed
    @ExceptionMetered
    public Response listRegisteredSmartLights() {
        return Response.ok(dao.findAll()).build();
    }

    @GET
    @Path("/{id}/status")
    @Timed
    @ExceptionMetered
    public Response currentStatus(@PathParam("id") long id) {
        var optionalLight = dao.findById(id);

        var light = optionalLight.orElseThrow(NotFoundException::new);

        return Response.ok(light).build();
    }

    @POST
    @Path("/register")
    @Timed
    @ExceptionMetered
    public Response registerThermostat(@BeanParam SmartLight light) {
        long id = dao.create(light);

        var uri = UriBuilder.fromUri("light/{id}/status").build(id);
        return Response.created(uri).entity(Map.of("id", id)).build();
    }

    @PUT
    @Path("/{id}/on")
    @Timed
    @ExceptionMetered
    public Response turnLightOn(@PathParam("id") long id) {
        dao.saveState(SmartLight.State.ON, id);
        return Response.accepted().build();
    }

    @PUT
    @Path("/{id}/off")
    @Timed
    @ExceptionMetered
    public Response turnLightOff(@PathParam("id") long id) {
        dao.saveState(SmartLight.State.OFF, id);
        return Response.accepted().build();
    }

    @PUT
    @Path("/{id}/color/{color}")
    @Timed
    @ExceptionMetered
    public Response setLightColor(@PathParam("id") long id, @PathParam("color") SmartLight.Color color) {
        dao.setColor(color, id);
        return Response.accepted().build();
    }

    @PUT
    @Path("/{id}/brightness/{brightness}")
    @Timed
    @ExceptionMetered
    public Response setLightBrightness(@PathParam("id") long id, @PathParam("brightness") int brightness) {
        dao.setBrightness(brightness, id);
        return Response.accepted().build();
    }

    @DELETE
    @Path("/{id}")
    @Timed
    @ExceptionMetered
    public Response deleteLight(@PathParam("id") long id) {
        dao.deleteLight(id);
        return Response.accepted().build();
    }

}
