package com.fortitudetec.elucidation.data.light.resource;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.joining;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.ElucidationEventRecorder;
import com.fortitudetec.elucidation.common.definition.HttpCommunicationDefinition;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import com.fortitudetec.elucidation.data.light.db.SmartLightDao;
import com.fortitudetec.elucidation.data.light.model.SmartLight;
import lombok.extern.slf4j.Slf4j;

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
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Path("/light")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class SmartLightResource {

    private static final String CONNECTION_IDENTIFIER_FORMAT = "%s %s";

    private SmartLightDao dao;
    private ElucidationClient<ResourceInfo> client;

    public SmartLightResource(SmartLightDao dao, ElucidationEventRecorder recorder) {
        this.dao = dao;

        var communicationDef = new HttpCommunicationDefinition();
        this.client = ElucidationClient.of(recorder, info -> Optional.of(ConnectionEvent.builder()
                .communicationType(communicationDef.getCommunicationType())
                .connectionIdentifier(connectionIdentifierFromResourceInfo(info))
                .eventDirection(Direction.INBOUND)
                .serviceName("light-service")
                .observedAt(System.currentTimeMillis())
                .build()));
    }

    @GET
    @Timed
    @ExceptionMetered
    public Response listRegisteredSmartLights(@Context ResourceInfo info) {
        recordEvent(info);

        return Response.ok(dao.findAll()).build();
    }

    @GET
    @Path("/{id}/status")
    @Timed
    @ExceptionMetered
    public Response currentStatus(@PathParam("id") long id, @Context ResourceInfo info) {
        recordEvent(info);

        var optionalLight = dao.findById(id);

        var light = optionalLight.orElseThrow(NotFoundException::new);

        return Response.ok(light).build();
    }

    @POST
    @Path("/register")
    @Timed
    @ExceptionMetered
    public Response registerThermostat(@BeanParam SmartLight light, @Context ResourceInfo info) {
        recordEvent(info);

        long id = dao.create(light);

        var uri = UriBuilder.fromUri("light/{id}/status").build(id);
        return Response.created(uri).entity(Map.of("id", id)).build();
    }

    @PUT
    @Path("/{id}/on")
    @Timed
    @ExceptionMetered
    public Response turnLightOn(@PathParam("id") long id, @Context ResourceInfo info) {
        recordEvent(info);

        dao.saveState(SmartLight.State.ON, id);
        return Response.accepted().build();
    }

    @PUT
    @Path("/{id}/off")
    @Timed
    @ExceptionMetered
    public Response turnLightOff(@PathParam("id") long id, @Context ResourceInfo info) {
        recordEvent(info);

        dao.saveState(SmartLight.State.OFF, id);
        return Response.accepted().build();
    }

    @PUT
    @Path("/{id}/color/{color}")
    @Timed
    @ExceptionMetered
    public Response setLightColor(@PathParam("id") long id, @PathParam("color") SmartLight.Color color, @Context ResourceInfo info) {
        recordEvent(info);

        dao.setColor(color, id);
        return Response.accepted().build();
    }

    @PUT
    @Path("/{id}/brightness/{brightness}")
    @Timed
    @ExceptionMetered
    public Response setLightBrightness(@PathParam("id") long id, @PathParam("brightness") int brightness, @Context ResourceInfo info) {
        recordEvent(info);

        dao.setBrightness(brightness, id);
        return Response.accepted().build();
    }

    @DELETE
    @Path("/{id}")
    @Timed
    @ExceptionMetered
    public Response deleteLight(@PathParam("id") long id, @Context ResourceInfo info) {
        recordEvent(info);

        dao.deleteLight(id);
        return Response.accepted().build();
    }

    private void recordEvent(ResourceInfo info) {
        client.recordNewEvent(info).whenComplete((result, exception) -> {
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
