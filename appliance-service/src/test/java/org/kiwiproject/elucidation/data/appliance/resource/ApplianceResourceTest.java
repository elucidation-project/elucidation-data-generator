package org.kiwiproject.elucidation.data.appliance.resource;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.kiwiproject.elucidation.client.ElucidationRecorder;
import org.kiwiproject.elucidation.client.ElucidationResult;
import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.data.appliance.db.ApplianceDao;
import org.kiwiproject.elucidation.data.appliance.model.Appliance;
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
@DisplayName("ApplianceResource")
@ExtendWith(DropwizardExtensionsSupport.class)
class ApplianceResourceTest {

    private static final ApplianceDao APPLIANCE_DAO = mock(ApplianceDao.class);
    private static final ElucidationRecorder RECORDER = mock(ElucidationRecorder.class);

    private static final DropwizardClientExtension RESOURCE
            = new DropwizardClientExtension(new ApplianceResource(APPLIANCE_DAO));
    private static final String NAME = "My First Appliance";

    private Client client;

    @BeforeEach
    void setUp() {
        client = ClientBuilder.newClient();
        when(RECORDER.recordNewEvent(any(ConnectionEvent.class))).thenReturn(CompletableFuture.completedFuture(ElucidationResult.ok()));
    }

    @AfterEach
    void clearMocks() {
        reset(APPLIANCE_DAO);
    }

    @Nested
    class ListRegisteredAppliance {

        @Test
        void shouldReturnAListOfAllAppliances() {
            var appliance = Appliance.builder()
                    .id(1L)
                    .name(NAME)
                    .brand("Nest")
                    .location("Kitchen")
                    .state(Appliance.State.OFF)
                    .build();

            when(APPLIANCE_DAO.findAll()).thenReturn(newArrayList(appliance));

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("appliance")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var appliances = response.readEntity(new GenericType<List<Appliance>>(){});

            assertThat(appliances)
                    .hasSize(1)
                    .extracting("id", "name", "brand", "location", "state")
                    .contains(tuple(
                            appliance.getId(),
                            appliance.getName(),
                            appliance.getBrand(),
                            appliance.getLocation(),
                            appliance.getState()));
        }

        @Test
        void shouldReturnEmptyListIfNoAppliances() {
            when(APPLIANCE_DAO.findAll()).thenReturn(new ArrayList<>());

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("appliance")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var appliances = response.readEntity(new GenericType<List<Appliance>>(){});

            assertThat(appliances).isEmpty();
        }
    }

    @Nested
    class FindAppliance {
        @Test
        void shouldReturnAppliance_WhenApplianceIsFound() {
            var appliance = Appliance.builder()
                    .id(1L)
                    .name(NAME)
                    .brand("Nest")
                    .location("Bedroom")
                    .state(Appliance.State.OFF)
                    .build();

            when(APPLIANCE_DAO.findById(1L)).thenReturn(Optional.of(appliance));

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("appliance/{id}/status")
                    .resolveTemplate("id", 1L)
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(Appliance.class)).isEqualToComparingFieldByField(appliance);
        }

        @Test
        void shouldReturn404_WhenApplianceIsNotFound() {
            when(APPLIANCE_DAO.findById(1L)).thenReturn(Optional.empty());

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("appliance/{id}/status")
                    .resolveTemplate("id", 1L)
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Nested
    class CreateAppliance {

        @Test
        void shouldReturn201_WithNewId() {
            var appliance = Appliance.builder()
                    .name(NAME)
                    .brand("Nest")
                    .location("Hallway")
                    .state(Appliance.State.OFF)
                    .build();

            when(APPLIANCE_DAO.create(any(Appliance.class))).thenReturn(1L);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("appliance/register")
                    .request()
                    .post(json(appliance));

            assertThat(response.getStatus()).isEqualTo(201);
            assertThat(response.getHeaderString("Location")).isEqualTo(RESOURCE.baseUri() + "/appliance/1/status");
            assertThat(response.readEntity(new GenericType<Map<String, Long>>(){}).get("id")).isEqualTo(1L);
        }
    }

    @Nested
    class DeleteAppliance {

        @Test
        void shouldDeleteAppliance() {
            when(APPLIANCE_DAO.deleteAppliance(1L)).thenReturn(1);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("appliance/{id}")
                    .resolveTemplate("id", 1L)
                    .request()
                    .delete();

            assertThat(response.getStatus()).isEqualTo(202);
            verify(APPLIANCE_DAO).deleteAppliance(1L);
        }
    }
}
