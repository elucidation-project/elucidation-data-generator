package com.fortitudetec.elucidation.data.home.resource;

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
import com.fortitudetec.elucidation.data.home.db.DeviceDao;
import com.fortitudetec.elucidation.data.home.model.Device;
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
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("java:S100")
@DisplayName("DeviceResource")
@ExtendWith(DropwizardExtensionsSupport.class)
class DeviceResourceTest {

    private static final DeviceDao DEVICE_DAO = mock(DeviceDao.class);
    private static final ElucidationRecorder RECORDER = mock(ElucidationRecorder.class);

    private static final DropwizardClientExtension RESOURCE
            = new DropwizardClientExtension(new DeviceResource(DEVICE_DAO, RECORDER));
    private static final String NAME = "My First Device";

    private Client client;

    @BeforeEach
    void setUp() {
        client = ClientBuilder.newClient();
        when(RECORDER.recordNewEvent(any(ConnectionEvent.class))).thenReturn(CompletableFuture.completedFuture(ElucidationResult.ok()));
    }

    @AfterEach
    void clearMocks() {
        reset(DEVICE_DAO);
    }

    @Nested
    class ListRegisteredDevice {

        @Test
        void shouldReturnAListOfAllDevice() {
            var device = Device.builder()
                    .id(1L)
                    .name(NAME)
                    .deviceType(Device.DeviceType.THERMOSTAT)
                    .deviceTypeId(10L)
                    .build();

            when(DEVICE_DAO.findAll()).thenReturn(newArrayList(device));

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("home/device")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var devices = response.readEntity(new GenericType<List<Device>>(){});

            assertThat(devices)
                    .hasSize(1)
                    .extracting("id", "name", "deviceType", "deviceTypeId")
                    .contains(tuple(
                            device.getId(),
                            device.getName(),
                            device.getDeviceType(),
                            device.getDeviceTypeId()));
        }

        @Test
        void shouldReturnEmptyListIfNoDevice() {
            when(DEVICE_DAO.findAll()).thenReturn(new ArrayList<>());

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("home/device")
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(200);

            var devices = response.readEntity(new GenericType<List<Device>>(){});

            assertThat(devices).isEmpty();
        }
    }

    @Nested
    class CreateDevice {

        @Test
        void shouldReturn201_WithNewId() {
            var device = Device.builder()
                    .name(NAME)
                    .deviceType(Device.DeviceType.THERMOSTAT)
                    .deviceTypeId(20L)
                    .build();

            when(DEVICE_DAO.create(any(Device.class))).thenReturn(1L);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("home/device/register")
                    .request()
                    .post(json(device));

            assertThat(response.getStatus()).isEqualTo(201);
            assertThat(response.readEntity(new GenericType<Map<String, Long>>(){}).get("id")).isEqualTo(1L);
        }
    }

    @Nested
    class DeleteDevice {

        @Test
        void shouldDeleteDevice() {
            when(DEVICE_DAO.deleteDevice(1L)).thenReturn(1);

            var response = client
                    .target(RESOURCE.baseUri())
                    .path("home/device/{id}")
                    .resolveTemplate("id", 1L)
                    .request()
                    .delete();

            assertThat(response.getStatus()).isEqualTo(202);
            verify(DEVICE_DAO).deleteDevice(1L);
        }
    }
}
