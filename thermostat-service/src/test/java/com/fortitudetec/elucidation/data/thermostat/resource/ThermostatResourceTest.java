package com.fortitudetec.elucidation.data.thermostat.resource;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fortitudetec.elucidation.data.thermostat.db.ThermostatDao;
import com.fortitudetec.elucidation.data.thermostat.model.Thermostat;
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

@SuppressWarnings("java:S100")
@DisplayName("ThermostatResource")
@ExtendWith(DropwizardExtensionsSupport.class)
class ThermostatResourceTest {

    private static final ThermostatDao THERMOSTAT_DAO = mock(ThermostatDao.class);

    private static final DropwizardClientExtension RESOURCE
            = new DropwizardClientExtension(new ThermostatResource(THERMOSTAT_DAO));
    private static final String NAME = "My First Thermostat";
    private static final String LOCATION = "Hallway";

    private Client client;

    @BeforeEach
    void setUp() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    void clearMocks() {
        reset(THERMOSTAT_DAO);
    }

    @Nested
    class ListRegisteredThermostats {

        @Test
        void shouldReturnAListOfAllThermostats() {
            var thermostat = Thermostat.builder()
                    .id(1L)
                    .name(NAME)
                    .brand("Nest")
                    .location(LOCATION)
                    .currentTemp(72.0)
                    .build();

            when(THERMOSTAT_DAO.findAll()).thenReturn(newArrayList(thermostat));

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("thermostat")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var thermostats = response.readEntity(new GenericType<List<Thermostat>>(){});

            assertThat(thermostats)
                    .hasSize(1)
                    .extracting("id", "name", "brand", "location", "currentTemp")
                    .contains(tuple(
                            thermostat.getId(),
                            thermostat.getName(),
                            thermostat.getBrand(),
                            thermostat.getLocation(),
                            thermostat.getCurrentTemp()));
        }

        @Test
        void shouldReturnEmptyListIfNoThermostats() {
            when(THERMOSTAT_DAO.findAll()).thenReturn(new ArrayList<>());

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("thermostat")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var thermostats = response.readEntity(new GenericType<List<Thermostat>>(){});

            assertThat(thermostats).isEmpty();
        }
    }

    @Nested
    class CurrentTemp {
        @Test
        void shouldReturnCurrentTemp_WhenThermostatIsFound() {
            var thermostat = Thermostat.builder()
                    .id(1L)
                    .name(NAME)
                    .brand("Nest")
                    .location(LOCATION)
                    .currentTemp(72.0)
                    .build();

            when(THERMOSTAT_DAO.findById(1L)).thenReturn(Optional.of(thermostat));

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("thermostat/{id}/status")
                    .resolveTemplate("id", 1L)
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(String.class)).isEqualTo("72.0");
        }

        @Test
        void shouldReturn404_WhenThermostatIsNotFound() {
            when(THERMOSTAT_DAO.findById(1L)).thenReturn(Optional.empty());

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("thermostat/{id}/status")
                    .resolveTemplate("id", 1L)
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    @Nested
    class CreateThermostat {

        @Test
        void shouldReturn201_WithNewId() {
            var thermostat = Thermostat.builder()
                    .name(NAME)
                    .brand("Nest")
                    .location(LOCATION)
                    .currentTemp(72.0)
                    .build();

            when(THERMOSTAT_DAO.create(any(Thermostat.class))).thenReturn(1L);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("thermostat/register")
                    .request()
                    .post(json(thermostat));

            assertThat(response.getStatus()).isEqualTo(201);
            assertThat(response.getHeaderString("Location")).isEqualTo(RESOURCE.baseUri() + "/thermostat/1/status");
            assertThat(response.readEntity(new GenericType<Map<String, Long>>(){}).get("id")).isEqualTo(1L);
        }
    }
}
