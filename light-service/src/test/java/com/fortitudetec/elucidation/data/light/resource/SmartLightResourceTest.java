package com.fortitudetec.elucidation.data.light.resource;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.client.ElucidationResult;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.data.light.db.SmartLightDao;
import com.fortitudetec.elucidation.data.light.model.SmartLight;
import io.dropwizard.testing.junit5.DropwizardClientExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("java:S100")
@DisplayName("SmartLightResource")
@ExtendWith(DropwizardExtensionsSupport.class)
class SmartLightResourceTest {

    private static final SmartLightDao SMART_LIGHT_DAO = mock(SmartLightDao.class);
    private static final ElucidationRecorder RECORDER = mock(ElucidationRecorder.class);

    private static final DropwizardClientExtension RESOURCE
            = new DropwizardClientExtension(new SmartLightResource(SMART_LIGHT_DAO));
    private static final String NAME = "My First Smart Light";
    private static final String LOCATION = "Kitchen";
    private static final String BRAND = "Phillips";

    private Client client;

    @BeforeEach
    void setUp() {
        client = ClientBuilder.newClient();
        when(RECORDER.recordNewEvent(any(ConnectionEvent.class))).thenReturn(CompletableFuture.completedFuture(ElucidationResult.ok()));
    }

    @AfterEach
    void clearMocks() {
        reset(SMART_LIGHT_DAO);
    }

    @Nested
    class ListRegisteredLights {

        @Test
        void shouldReturnAListOfAllLights() {
            var light = SmartLight.builder()
                    .id(1L)
                    .name(NAME)
                    .brand(BRAND)
                    .location(LOCATION)
                    .state(SmartLight.State.OFF)
                    .color(SmartLight.Color.BLUE)
                    .brightness(50)
                    .build();

            when(SMART_LIGHT_DAO.findAll()).thenReturn(newArrayList(light));

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("light")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var lights = response.readEntity(new GenericType<List<SmartLight>>(){});

            assertThat(lights)
                    .hasSize(1)
                    .extracting("id", "name", "brand", "location", "state", "color", "brightness")
                    .contains(tuple(
                            light.getId(),
                            light.getName(),
                            light.getBrand(),
                            light.getLocation(),
                            light.getState(),
                            light.getColor(),
                            light.getBrightness()));
        }

        @Test
        void shouldReturnEmptyListIfNoLights() {
            when(SMART_LIGHT_DAO.findAll()).thenReturn(new ArrayList<>());

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("light")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var lights = response.readEntity(new GenericType<List<SmartLight>>(){});

            assertThat(lights).isEmpty();
        }
    }

    @Nested
    class CurrentStatus {
        @Test
        void shouldReturnSmartLight_WhenLightIsFound() {
            var light = SmartLight.builder()
                    .id(1L)
                    .name(NAME)
                    .brand(BRAND)
                    .location(LOCATION)
                    .state(SmartLight.State.ON)
                    .color(SmartLight.Color.ORANGE)
                    .brightness(20)
                    .build();

            when(SMART_LIGHT_DAO.findById(1L)).thenReturn(Optional.of(light));

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("light/{id}/status")
                    .resolveTemplate("id", 1L)
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var returnedLight = response.readEntity(SmartLight.class);

            assertThat(returnedLight).isEqualToComparingFieldByField(light);
        }

        @Test
        void shouldReturn404_WhenLightIsNotFound() {
            when(SMART_LIGHT_DAO.findById(1L)).thenReturn(Optional.empty());

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("light/{id}/status")
                    .resolveTemplate("id", 1L)
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Nested
    class CreateSmartLight {

        @Test
        void shouldReturn201_WithNewId() {
            var light = SmartLight.builder()
                    .name(NAME)
                    .brand(BRAND)
                    .location(LOCATION)
                    .state(SmartLight.State.OFF)
                    .color(SmartLight.Color.GREEN)
                    .brightness(45)
                    .build();

            when(SMART_LIGHT_DAO.create(any(SmartLight.class))).thenReturn(1L);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("light/register")
                    .request()
                    .post(json(light));

            assertThat(response.getStatus()).isEqualTo(201);
            assertThat(response.getHeaderString("Location")).isEqualTo(RESOURCE.baseUri() + "/light/1/status");
            assertThat(response.readEntity(new GenericType<Map<String, Long>>(){}).get("id")).isEqualTo(1L);
        }
    }

    @Nested
    class UpdateState {

        @Test
        void shouldTurnLightOff_WhenOffIsRequested() {
            when(SMART_LIGHT_DAO.saveState(SmartLight.State.OFF, 1L)).thenReturn(1);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("light/{id}/off")
                    .resolveTemplate("id", 1L)
                    .request()
                    .put(json(""));

            assertThat(response.getStatus()).isEqualTo(202);
            verify(SMART_LIGHT_DAO).saveState(SmartLight.State.OFF, 1L);
        }

        @Test
        void shouldTurnLightOn_WhenOnIsRequested() {
            when(SMART_LIGHT_DAO.saveState(SmartLight.State.ON, 1L)).thenReturn(1);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("light/{id}/on")
                    .resolveTemplate("id", 1L)
                    .request()
                    .put(json(""));

            assertThat(response.getStatus()).isEqualTo(202);
            verify(SMART_LIGHT_DAO).saveState(SmartLight.State.ON, 1L);
        }
    }

    @Nested
    class SetLightColor {

        @Test
        void shouldSetLightColor() {
            when(SMART_LIGHT_DAO.setColor(SmartLight.Color.GREEN, 1L)).thenReturn(1);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("light/{id}/color/{color}")
                    .resolveTemplate("id", 1L)
                    .resolveTemplate("color", "GREEN")
                    .request()
                    .put(json(""));

            assertThat(response.getStatus()).isEqualTo(202);
            verify(SMART_LIGHT_DAO).setColor(SmartLight.Color.GREEN, 1L);
        }
    }

    @Nested
    class SetLightBrightness {

        @Test
        void shouldSetLightBrightness() {
            when(SMART_LIGHT_DAO.setBrightness(45, 1L)).thenReturn(1);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("light/{id}/brightness/{brightness}")
                    .resolveTemplate("id", 1L)
                    .resolveTemplate("brightness", 45)
                    .request()
                    .put(json(""));

            assertThat(response.getStatus()).isEqualTo(202);
            verify(SMART_LIGHT_DAO).setBrightness(45, 1L);
        }
    }

    @Nested
    class DeleteLight {

        @Test
        void shouldDeleteLight() {
            when(SMART_LIGHT_DAO.deleteLight(1L)).thenReturn(1);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("light/{id}")
                    .resolveTemplate("id", 1L)
                    .request()
                    .delete();

            assertThat(response.getStatus()).isEqualTo(202);
            verify(SMART_LIGHT_DAO).deleteLight(1L);
        }
    }
}
