package com.fortitudetec.elucidation.data.doorbell.resource;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fortitudetec.elucidation.client.ElucidationEventRecorder;
import com.fortitudetec.elucidation.client.RecorderResult;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.data.doorbell.db.DoorbellDao;
import com.fortitudetec.elucidation.data.doorbell.model.Doorbell;
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
@DisplayName("DoorbellResource")
@ExtendWith(DropwizardExtensionsSupport.class)
class DoorbellResourceTest {

    private static final DoorbellDao DOORBELL_DAO = mock(DoorbellDao.class);
    private static final ElucidationEventRecorder RECORDER = mock(ElucidationEventRecorder.class);

    private static final DropwizardClientExtension RESOURCE
            = new DropwizardClientExtension(new DoorbellResource(DOORBELL_DAO, RECORDER));
    private static final String NAME = "My First Doorbell";

    private Client client;

    @BeforeEach
    void setUp() {
        client = ClientBuilder.newClient();
        when(RECORDER.recordNewEvent(any(ConnectionEvent.class))).thenReturn(CompletableFuture.completedFuture(RecorderResult.ok()));
    }

    @AfterEach
    void clearMocks() {
        reset(DOORBELL_DAO);
    }

    @Nested
    class ListRegisteredDoorbell {

        @Test
        void shouldReturnAListOfAllDoorbells() {
            var doorbell = Doorbell.builder()
                    .id(1L)
                    .name(NAME)
                    .brand("Nest")
                    .build();

            when(DOORBELL_DAO.findAll()).thenReturn(newArrayList(doorbell));

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("doorbell")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var doorbells = response.readEntity(new GenericType<List<Doorbell>>(){});

            assertThat(doorbells)
                    .hasSize(1)
                    .extracting("id", "name", "brand")
                    .contains(tuple(
                            doorbell.getId(),
                            doorbell.getName(),
                            doorbell.getBrand()));
        }

        @Test
        void shouldReturnEmptyListIfNoDoorbells() {
            when(DOORBELL_DAO.findAll()).thenReturn(new ArrayList<>());

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("doorbell")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var doorbells = response.readEntity(new GenericType<List<Doorbell>>(){});

            assertThat(doorbells).isEmpty();
        }
    }

    @Nested
    class FindDoorbell {
        @Test
        void shouldReturnDoorbell_WhenDoorbellIsFound() {
            var doorbell = Doorbell.builder()
                    .id(1L)
                    .name(NAME)
                    .brand("Nest")
                    .build();

            when(DOORBELL_DAO.findById(1L)).thenReturn(Optional.of(doorbell));

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("doorbell/{id}/status")
                    .resolveTemplate("id", 1L)
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(Doorbell.class)).isEqualToComparingFieldByField(doorbell);
        }

        @Test
        void shouldReturn404_WhenDoorbellIsNotFound() {
            when(DOORBELL_DAO.findById(1L)).thenReturn(Optional.empty());

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("doorbell/{id}/status")
                    .resolveTemplate("id", 1L)
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Nested
    class CreateDoorbell {

        @Test
        void shouldReturn201_WithNewId() {
            var doorbell = Doorbell.builder()
                    .name(NAME)
                    .brand("Nest")
                    .build();

            when(DOORBELL_DAO.create(any(Doorbell.class))).thenReturn(1L);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("doorbell/register")
                    .request()
                    .post(json(doorbell));

            assertThat(response.getStatus()).isEqualTo(201);
            assertThat(response.getHeaderString("Location")).isEqualTo(RESOURCE.baseUri() + "/doorbell/1/status");
            assertThat(response.readEntity(new GenericType<Map<String, Long>>(){}).get("id")).isEqualTo(1L);
        }
    }

    @Nested
    class RingDoorbell {

        @Test
        void shouldReturn202_WhenDoorbellIsRung() {
            var response = client
                    .target(RESOURCE.baseUri())
                    .path("doorbell/{id}/ring")
                    .resolveTemplate("id", 1L)
                    .request()
                    .post(json(""));

            assertThat(response.getStatus()).isEqualTo(202);
        }
    }

    @Nested
    class DeleteDoorbell {

        @Test
        void shouldDeleteDoorbell() {
            when(DOORBELL_DAO.deleteDoorbell(1L)).thenReturn(1);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("doorbell/{id}")
                    .resolveTemplate("id", 1L)
                    .request()
                    .delete();

            assertThat(response.getStatus()).isEqualTo(202);
            verify(DOORBELL_DAO).deleteDoorbell(1L);
        }
    }
}
