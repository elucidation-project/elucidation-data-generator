package com.fortitudetec.elucidation.data.thermostat.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fortitudetec.elucidation.data.thermostat.db.ThermostatDao;
import com.fortitudetec.elucidation.data.thermostat.model.Thermostat;

import javax.validation.constraints.NotNull;
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

@Path("/thermostat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ThermostatResource {

    private final ThermostatDao dao;

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
    public Response currentTemp(@PathParam("id") long id) {
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
    public Response registerThermostat(@NotNull Thermostat thermostat) {
        long id = dao.create(thermostat);

        var uri = UriBuilder.fromUri("thermostat/{id}/status").build(id);
        return Response.created(uri).entity(Map.of("id", id)).build();
    }

    @PUT
    @Path("/{id}/temp")
    @Timed
    @ExceptionMetered
    public Response adjustTemperatureFor(@PathParam("id") long id, @NotNull Map<String, Double> body) {
        if (!body.containsKey("temp")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Body must contain temp:<temperature>")).build();
        }

        var updatedCount = dao.setCurrentTemp(body.get("temp"), id);

        if (updatedCount == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.accepted().build();
    }

    @DELETE
    @Path("/{id}")
    @Timed
    @ExceptionMetered
    public Response deleteThermostat(@PathParam("id") long id) {
        dao.deleteThermostat(id);
        return Response.accepted().build();
    }

}
