package org.kiwiproject.elucidation.data.home.resource;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.kiwiproject.elucidation.data.home.db.WorkflowDao;
import org.kiwiproject.elucidation.data.home.model.Workflow;
import org.kiwiproject.elucidation.data.home.service.WorkflowService;
import lombok.extern.slf4j.Slf4j;

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
import java.util.Map;

@Path("/home/workflow")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class WorkflowResource {

    private final WorkflowDao dao;
    private final WorkflowService workflowService;

    public WorkflowResource(WorkflowDao dao, WorkflowService workflowService) {
        this.dao = dao;
        this.workflowService = workflowService;
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
    public Response createWorkflow(@NotNull Workflow workflow) {
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

    @PUT
    @Path("trigger/byId/{id}")
    @Timed
    @ExceptionMetered
    public Response triggerWorkflowById(@PathParam("id") long id) {
        var optionalWorkflow = dao.findById(id);

        var workflow = optionalWorkflow.orElseThrow(() -> new NotFoundException("Can't find workflow"));

        LOG.info("Triggering workflow {}", workflow.getName());
        workflowService.runWorkflow(workflow);

        return Response.accepted().build();
    }

    @PUT
    @Path("trigger/byName/{name}")
    @Timed
    @ExceptionMetered
    public Response triggerWorkflowByName(@PathParam("name") String name) {
        var optionalWorkflow = dao.findByName(name);

        var workflow = optionalWorkflow.orElseThrow(() -> new NotFoundException("Can't find workflow"));

        LOG.info("Triggering workflow {}", workflow.getName());
        workflowService.runWorkflow(workflow);

        return Response.accepted().build();
    }

}
