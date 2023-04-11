package org.kiwiproject.elucidation.data.appliance.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.kiwiproject.elucidation.data.appliance.db.ApplianceDao;
import org.kiwiproject.elucidation.data.appliance.model.Appliance;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
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

@Path("/appliance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ApplianceResource {

    private final ApplianceDao dao;

    public ApplianceResource(ApplianceDao dao) {
        this.dao = dao;
    }

    @GET
    @Timed
    @ExceptionMetered
    public Response listRegisteredAppliances() {
        return Response.ok(dao.findAll()).build();
    }

    @GET
    @Path("/{id}/status")
    @Timed
    @ExceptionMetered
    public Response findAppliance(@PathParam("id") long id) {
        var optionalAppliance = dao.findById(id);
        return Response.ok(optionalAppliance.orElseThrow(NotFoundException::new)).build();
    }

    @POST
    @Path("/register")
    @Timed
    @ExceptionMetered
    public Response registerAppliance(@NotNull Appliance appliance) {
        long id = dao.create(appliance);

        var uri = UriBuilder.fromUri("appliance/{id}/status").build(id);
        return Response.created(uri).entity(Map.of("id", id)).build();
    }

    @DELETE
    @Path("/{id}")
    @Timed
    @ExceptionMetered
    public Response deleteAppliance(@PathParam("id") long id) {
        dao.deleteAppliance(id);
        return Response.accepted().build();
    }

}
