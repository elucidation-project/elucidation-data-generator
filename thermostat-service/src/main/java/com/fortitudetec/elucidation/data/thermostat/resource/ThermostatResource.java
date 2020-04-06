package com.fortitudetec.elucidation.data.thermostat.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fortitudetec.elucidation.data.thermostat.db.ThermostatDao;
import com.fortitudetec.elucidation.data.thermostat.model.Thermostat;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
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

@Path("/thermostat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ThermostatResource {

    private ThermostatDao dao;

    public ThermostatResource(ThermostatDao dao) {
        this.dao = dao;
    }

    @GET
    @Timed
    @ExceptionMetered
    public Response listRegisteredThermostats() {
        return Response.ok(dao.findAll()).build();
    }

    @GET
    @Path("/{id}/status")
    @Timed
    @ExceptionMetered
    public Response currentTemp(@PathParam("id") Long id) {
        var optionalThermostat = dao.findById(id);

        return Response.ok(optionalThermostat
                .orElseThrow(NotFoundException::new)
                .getCurrentTemp())
                .build();
    }

    @POST
    @Path("/register")
    @Timed
    @ExceptionMetered
    public Response registerThermostat(@BeanParam Thermostat thermostat) {
        long id = dao.create(thermostat);

        var uri = UriBuilder.fromUri("thermostat/{id}/status").build(id);
        return Response.created(uri).entity(Map.of("id", id)).build();
    }


}
