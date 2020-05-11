package com.fortitudetec.elucidation.data.home.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fortitudetec.elucidation.data.home.db.WorkflowDao;
import com.fortitudetec.elucidation.data.home.model.Workflow;

import javax.ws.rs.BeanParam;
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

@Path("/home/workflow")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkflowResource {

    private WorkflowDao dao;

    public WorkflowResource(WorkflowDao dao) {
        this.dao = dao;
    }

    @GET
    @Timed
    @ExceptionMetered
    public Response listWorkflows() {
        return Response.ok(dao.findAll()).build();
    }

    @POST
    @Timed
    @ExceptionMetered
    public Response createWorkflow(@BeanParam Workflow workflow) {
        long id = dao.create(workflow);

        return Response.status(201).entity(Map.of("id", id)).build();
    }

    @DELETE
    @Path("/{id}")
    @Timed
    @ExceptionMetered
    public Response deleteWorkflow(@PathParam("id") long id) {
        dao.deleteWorkflow(id);
        return Response.accepted().build();
    }
}
