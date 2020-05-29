package com.fortitudetec.elucidation.data.home.resource;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.apache.commons.lang3.StringUtils.stripStart;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.ElucidationEventRecorder;
import com.fortitudetec.elucidation.common.definition.HttpCommunicationDefinition;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import com.fortitudetec.elucidation.data.home.db.WorkflowDao;
import com.fortitudetec.elucidation.data.home.model.Workflow;
import com.fortitudetec.elucidation.data.home.service.WorkflowService;
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
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Path("/home/workflow")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class WorkflowResource {

    private static final String CONNECTION_IDENTIFIER_FORMAT = "%s /%s";

    private final WorkflowDao dao;
    private final ElucidationClient<ResourceInfo> client;
    private final WorkflowService workflowService;

    public WorkflowResource(WorkflowDao dao, ElucidationEventRecorder recorder, WorkflowService workflowService) {
        this.dao = dao;
        this.workflowService = workflowService;
        var communicationDef = new HttpCommunicationDefinition();
        this.client = ElucidationClient.of(recorder, info -> Optional.of(ConnectionEvent.builder()
                .communicationType(communicationDef.getCommunicationType())
                .connectionIdentifier(connectionIdentifierFromResourceInfo(info))
                .eventDirection(Direction.INBOUND)
                .serviceName("home-service")
                .observedAt(System.currentTimeMillis())
                .build()));
    }

    @GET
    @Timed
    @ExceptionMetered
    public Response listWorkflows(@Context ResourceInfo info) {
        recordEvent(info);

        return Response.ok(dao.findAll()).build();
    }

    @POST
    @Timed
    @ExceptionMetered
    public Response createWorkflow(@NotNull Workflow workflow, @Context ResourceInfo info) {
        recordEvent(info);

        long id = dao.create(workflow);

        return Response.status(201).entity(Map.of("id", id)).build();
    }

    @DELETE
    @Path("/{id}")
    @Timed
    @ExceptionMetered
    public Response deleteWorkflow(@PathParam("id") long id, @Context ResourceInfo info) {
        recordEvent(info);

        dao.deleteWorkflow(id);
        return Response.accepted().build();
    }

    @PUT
    @Path("trigger/byId/{id}")
    @Timed
    @ExceptionMetered
    public Response triggerWorkflowById(@PathParam("id") long id, @Context ResourceInfo info) {
        recordEvent(info);

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
    public Response triggerWorkflowByName(@PathParam("name") String name, @Context ResourceInfo info) {
        recordEvent(info);

        var optionalWorkflow = dao.findByName(name);

        var workflow = optionalWorkflow.orElseThrow(() -> new NotFoundException("Can't find workflow"));

        LOG.info("Triggering workflow {}", workflow.getName());
        workflowService.runWorkflow(workflow);

        return Response.accepted().build();
    }

    private void recordEvent(ResourceInfo info) {
        client.recordNewEvent(info).whenComplete((result, exception) -> {
            if (nonNull(exception)) {
                LOG.error("An error occurred recording an event.", exception);
                return;
            }

            switch (result.getStatus()) {
                case RECORDED_OK:
                    LOG.info("Successfully recorded event to Elucidation");
                    break;
                case SKIPPED_RECORDING:
                    LOG.info("Recording was skipped.  Shouldn't happen here");
                    break;
                case ERROR_RECORDING:
                    LOG.error("Had a problem recording event. Error: {} Exception: {}", result.getErrorMessage(), result.getException());
            }
        });
    }

    private String connectionIdentifierFromResourceInfo(ResourceInfo info) {
        var rootPath = Optional.ofNullable(info.getResourceClass())
                .map(aClass -> aClass.getDeclaredAnnotation(Path.class))
                .orElse(null);

        var methodPath = Optional.ofNullable(info.getResourceMethod())
                .map(aMethod -> aMethod.getDeclaredAnnotation(Path.class))
                .orElse(null);

        var templatedPath = newArrayList(rootPath, methodPath).stream()
                .filter(Objects::nonNull)
                .map(Path::value)
                .map(path -> stripStart(path, "/"))
                .map(path -> stripEnd(path, "/"))
                .collect(joining("/"));

        var method = Optional.ofNullable(info.getResourceMethod())
                .map(aMethod -> Stream.of(aMethod.getDeclaredAnnotations())
                        .filter(annotation -> annotation instanceof GET || annotation instanceof PUT || annotation instanceof POST || annotation instanceof DELETE)
                        .findFirst())
                .map(Optional::get)
                .map(annotation -> annotation.annotationType().getSimpleName())
                .orElse("UNKNOWN");

        return String.format(CONNECTION_IDENTIFIER_FORMAT, method, templatedPath);
    }
}
