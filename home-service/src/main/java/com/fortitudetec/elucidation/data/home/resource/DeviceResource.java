package com.fortitudetec.elucidation.data.home.resource;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.ElucidationEventRecorder;
import com.fortitudetec.elucidation.common.definition.HttpCommunicationDefinition;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import com.fortitudetec.elucidation.data.home.db.DeviceDao;
import com.fortitudetec.elucidation.data.home.model.Device;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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

@Path("/home/device")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class DeviceResource {

    private static final String CONNECTION_IDENTIFIER_FORMAT = "%s %s";

    private final DeviceDao dao;
    private final ElucidationClient<ResourceInfo> client;

    public DeviceResource(DeviceDao dao, ElucidationEventRecorder recorder) {
        this.dao = dao;

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
    public Response listRegisteredDevices(@Context ResourceInfo info) {
        recordEvent(info);

        return Response.ok(dao.findAll()).build();
    }

    @POST
    @Path("/register")
    @Timed
    @ExceptionMetered
    public Response registerDevice(@BeanParam Device device, @Context ResourceInfo info) {
        recordEvent(info);

        long id = dao.create(device);

        return Response.status(201).entity(Map.of("id", id)).build();
    }

    @DELETE
    @Path("/{id}")
    @Timed
    @ExceptionMetered
    public Response deleteDevice(@PathParam("id") long id, @Context ResourceInfo info) {
        recordEvent(info);

        dao.deleteDevice(id);
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
