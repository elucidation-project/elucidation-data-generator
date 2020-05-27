package com.fortitudetec.elucidation.data.thermostat.resource;

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
import com.fortitudetec.elucidation.data.thermostat.db.ThermostatDao;
import com.fortitudetec.elucidation.data.thermostat.model.Thermostat;
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

@Path("/thermostat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ThermostatResource {

    private static final String CONNECTION_IDENTIFIER_FORMAT = "%s /%s";

    private final ThermostatDao dao;
    private final ElucidationClient<ResourceInfo> client;

    public ThermostatResource(ThermostatDao dao, ElucidationEventRecorder recorder) {
        this.dao = dao;

        var communicationDef = new HttpCommunicationDefinition();
        this.client = ElucidationClient.of(recorder, info -> Optional.of(ConnectionEvent.builder()
                .communicationType(communicationDef.getCommunicationType())
                .connectionIdentifier(connectionIdentifierFromResourceInfo(info))
                .eventDirection(Direction.INBOUND)
                .serviceName("thermostat-service")
                .observedAt(System.currentTimeMillis())
                .build()));
    }

    @GET
    @Timed
    @ExceptionMetered
    public Response listRegisteredThermostats(@Context ResourceInfo info) {
        recordEvent(info);

        return Response.ok(dao.findAll()).build();
    }

    @GET
    @Path("/{id}/status")
    @Timed
    @ExceptionMetered
    public Response currentTemp(@PathParam("id") long id, @Context ResourceInfo info) {
        recordEvent(info);

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
    public Response registerThermostat(@NotNull Thermostat thermostat, @Context ResourceInfo info) {
        recordEvent(info);

        long id = dao.create(thermostat);

        var uri = UriBuilder.fromUri("thermostat/{id}/status").build(id);
        return Response.created(uri).entity(Map.of("id", id)).build();
    }

    @PUT
    @Path("/{id}/temp")
    @Timed
    @ExceptionMetered
    public Response adjustTemperatureFor(@PathParam("id") long id, @NotNull Map<String, Double> body, @Context ResourceInfo info) {
        recordEvent(info);

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
    public Response deleteThermostat(@PathParam("id") long id, @Context ResourceInfo info) {
        recordEvent(info);

        dao.deleteThermostat(id);
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
