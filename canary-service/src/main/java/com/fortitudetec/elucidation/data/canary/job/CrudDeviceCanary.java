package com.fortitudetec.elucidation.data.canary.job;

import static javax.ws.rs.client.Entity.json;

import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.common.definition.HttpCommunicationDefinition;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class CrudDeviceCanary {

    private static final String BRAND = "brand";
    private static final String LOCATION = "location";
    private static final String DEVICE_TYPE_ID = "deviceTypeId";
    private final Client httpClient;
    private final ElucidationClient<String> client;

    public CrudDeviceCanary(Client httpClient, ElucidationRecorder eventRecorder) {
        this.httpClient = httpClient;

        var communicationDef = new HttpCommunicationDefinition();
        this.client = ElucidationClient.of(eventRecorder, identifier -> Optional.of(ConnectionEvent.builder()
                .communicationType(communicationDef.getCommunicationType())
                .connectionIdentifier(identifier)
                .eventDirection(Direction.OUTBOUND)
                .serviceName("canary-service")
                .observedAt(System.currentTimeMillis())
                .build()));
    }

    public void runCanaryTest() {
        LOG.info("**********************************************************");
        LOG.info("* Running canary test to perform CRUD actions on devices *");
        LOG.info("**********************************************************");

        // Register 2 thermostats
        createAndRegisterThermostat("Main Floor Nest Thermostat", "Hallway");
        createAndRegisterThermostat("Top Floor Nest Thermostat", "Bedroom");

        // Register 5 lights
        createAndRegisterLight("Master Bedroom Light", "Master");
        createAndRegisterLight("Guest Bedroom Light", "Guest");
        createAndRegisterLight("Kitchen Light", "Kitchen");
        createAndRegisterLight("Bathroom Light", "Bathroom");
        var pantryLightId = createAndRegisterLight("Pantry Light", "Pantry");

        // Register 1 doorbell
        createAndRegisterDoorbell();

        // Register 1 coffee machine
        createAndRegisterAppliance();

        // Delete 1 light
        retrieveAndRemoveLight(pantryLightId);
    }

    private void createAndRegisterThermostat(String name, String location) {
        client.recordNewEvent("POST /thermostat/register");
        var thermostatResponse = httpClient.target("http://thermostat:8080/thermostat/register")
                .request()
                .post(json(Map.of("name", name, BRAND, "Nest", LOCATION, location, "currentTemp", 0.0)));

        if (thermostatResponse.getStatus() == 201) {
            var id = thermostatResponse.readEntity(new GenericType<Map<String, Long>>(){}).get("id");
            registerDevice(name, id, "THERMOSTAT");
        } else {
            LOG.warn("Unable to save thermostat. Status: {} Body: {}", thermostatResponse.getStatus(), thermostatResponse.readEntity(String.class));
        }
    }

    private int createAndRegisterLight(String name, String location) {
        client.recordNewEvent("POST /light/register");
        var lightResponse = httpClient.target("http://light:8080/light/register")
                .request()
                .post(json(Map.of("name", name,
                        BRAND, "Phillips",
                        LOCATION, location,
                        "state", "OFF",
                        "color", "SOFT_WHITE",
                        "brightness", 100)));

        if (lightResponse.getStatus() == 201) {
            var id = lightResponse.readEntity(new GenericType<Map<String, Long>>(){}).get("id");
            return registerDevice(name, id, "LIGHT");
        } else {
            LOG.warn("Unable to save light. Status: {} Body: {}", lightResponse.getStatus(), lightResponse.readEntity(String.class));
        }

        return -1;
    }

    private void createAndRegisterDoorbell() {
        client.recordNewEvent("POST /doorbell/register");
        var doorbellResponse = httpClient.target("http://doorbell:8080/doorbell/register")
                .request()
                .post(json(Map.of("name", "Front Doorbell", BRAND, "Ring")));

        if (doorbellResponse.getStatus() == 201) {
            var id = doorbellResponse.readEntity(new GenericType<Map<String, Long>>(){}).get("id");
            registerDevice("Front Doorbell", id, "DOORBELL");
        } else {
            LOG.warn("Unable to save doorbell. Status: {} Body: {}", doorbellResponse.getStatus(), doorbellResponse.readEntity(String.class));
        }
    }

    private void createAndRegisterAppliance() {
        client.recordNewEvent("POST /appliance/register");
        var applianceResponse = httpClient.target("http://appliance:8080/appliance/register")
                .request()
                .post(json(Map.of("name", "Coffee Machine", BRAND, "Kuerig", LOCATION, "Kitchen", "state", "OFF")));

        if (applianceResponse.getStatus() == 201) {
            var id = applianceResponse.readEntity(new GenericType<Map<String, Long>>(){}).get("id");
            registerDevice("Coffee Machine", id, "APPLIANCE");
        } else {
            LOG.warn("Unable to save appliance. Status: {} Body: {}", applianceResponse.getStatus(), applianceResponse.readEntity(String.class));
        }
    }

    private int registerDevice(String name, long id, String type) {
        client.recordNewEvent("POST /home/device/register");
        var response = httpClient.target("http://home:8080/home/device/register")
                .request()
                .post(json(Map.of("name", name, "deviceType", type, DEVICE_TYPE_ID, id)));

        if (response.getStatus() == 201) {
            var deviceId = response.readEntity(new GenericType<Map<String, Integer>>(){}).get("id");

            LOG.info("{} {} created with id: {}", type, name, deviceId);
            return deviceId;
        } else {
            LOG.warn("Unable to save device. Status: {} Body: {}", response.getStatus(), response.readEntity(String.class));
        }

        return -1;
    }

    private void retrieveAndRemoveLight(int deviceId) {
        client.recordNewEvent("GET /home/device");
        var deviceResponse = httpClient.target("http://home:8080/home/device")
                .request()
                .get();

        if (deviceResponse.getStatus() == 200) {
            LOG.info("Retrieved all devices");
            var device = deviceResponse.readEntity(new GenericType<List<Map<String, Object>>>(){}).stream()
                    .filter(deviceMap -> deviceId == (int) deviceMap.get("id"))
                    .findFirst()
                    .orElseThrow();

            LOG.info("Found light to delete. {}", device.get(DEVICE_TYPE_ID));
            client.recordNewEvent("DELETE /light/{id}");
            var lightResponse = httpClient.target("http://light:8080/light/{id}")
                    .resolveTemplate("id", device.get(DEVICE_TYPE_ID))
                    .request()
                    .delete();

            if (lightResponse.getStatus() == 202) {
                LOG.info("Light has been deleted");
            } else {
                LOG.warn("Unable to delete light. Status: {} Body: {}", lightResponse.getStatus(), lightResponse.readEntity(String.class));
            }

            client.recordNewEvent("DELETE /home/device/{id}");
            var deviceDeleteResponse = httpClient.target("http://home:8080/home/device/{id}")
                    .resolveTemplate("id", deviceId)
                    .request()
                    .delete();

            if (deviceDeleteResponse.getStatus() == 202) {
                LOG.info("Light Device has been deleted from home");
            } else {
                LOG.warn("Unable to delete light from home. Status: {} Body: {}", deviceDeleteResponse.getStatus(), deviceDeleteResponse.readEntity(String.class));
            }
        } else {
            LOG.warn("Unable to retrieve devices. Won't be able to remove the light. Status: {} Body: {}",
                    deviceResponse.getStatus(), deviceResponse.readEntity(String.class));
        }
    }
}
