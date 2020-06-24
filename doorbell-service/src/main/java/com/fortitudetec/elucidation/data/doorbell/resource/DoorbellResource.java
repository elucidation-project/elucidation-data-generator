package com.fortitudetec.elucidation.data.doorbell.resource;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.apache.commons.lang3.StringUtils.stripStart;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.common.definition.HttpCommunicationDefinition;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import com.fortitudetec.elucidation.data.doorbell.db.DoorbellDao;
import com.fortitudetec.elucidation.data.doorbell.model.Doorbell;
import com.fortitudetec.elucidation.data.doorbell.service.DoorbellService;
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
import javax.ws.rs.core.UriBuilder;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Path("/doorbell")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class DoorbellResource {

    private static final String CONNECTION_IDENTIFIER_FORMAT = "%s /%s";

    private final DoorbellDao dao;
    private final ElucidationClient<ResourceInfo> inboundClient;
    private final DoorbellService service;

    public DoorbellResource(DoorbellDao dao, ElucidationRecorder recorder, DoorbellService service) {
        this.dao = dao;
        this.service = service;

        var communicationDef = new HttpCommunicationDefinition();
        this.inboundClient = ElucidationClient.of(recorder, info -> Optional.of(ConnectionEvent.builder()
                .communicationType(communicationDef.getCommunicationType())
                .connectionIdentifier(connectionIdentifierFromResourceInfo(info))
                .eventDirection(Direction.INBOUND)
                .serviceName("doorbell-service")
                .observedAt(System.currentTimeMillis())
                .build()));

    }

    @GET
    @Timed
    @ExceptionMetered
    public Response listRegisteredDoorbells(@Context ResourceInfo info) {
        recordEvent(info);

        return Response.ok(dao.findAll()).build();
    }

    @GET
    @Path("/{id}/status")
    @Timed
    @ExceptionMetered
    public Response findDoorbell(@PathParam("id") long id, @Context ResourceInfo info) {
        recordEvent(info);

        var optionalDoorbell = dao.findById(id);
        return Response.ok(optionalDoorbell.orElseThrow(NotFoundException::new)).build();
    }

    @POST
    @Path("/register")
    @Timed
    @ExceptionMetered
    public Response registerDoorbell(@NotNull Doorbell doorbell, @Context ResourceInfo info) {
        recordEvent(info);

        long id = dao.create(doorbell);

        var uri = UriBuilder.fromUri("doorbell/{id}/status").build(id);
        return Response.created(uri).entity(Map.of("id", id)).build();
    }

    @DELETE
    @Path("/{id}")
    @Timed
    @ExceptionMetered
    public Response deleteDoorbell(@PathParam("id") long id, @Context ResourceInfo info) {
        recordEvent(info);

        dao.deleteDoorbell(id);
        return Response.accepted().build();
    }

    @POST
    @Path("/{id}/ring")
    @Timed
    @ExceptionMetered
    public Response ringDoorbell(@PathParam("id") long id, @Context ResourceInfo info) {
        recordEvent(info);
        service.ringDoorbell();
        return Response.accepted().build();
    }

    private void recordEvent(ResourceInfo info) {
        inboundClient.recordNewEvent(info).whenComplete((result, exception) -> {
            if (nonNull(exception)) {
                LOG.error("An error occurred recording an event.", exception);
                return;
            }

            switch (result.getStatus()) {
                case SUCCESS:
                    LOG.info("Successfully recorded event to Elucidation");
                    break;
                case SKIPPED:
                    LOG.info("Recording was skipped.  Shouldn't happen here");
                    break;
                case ERROR:
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
