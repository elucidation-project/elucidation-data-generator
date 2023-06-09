package org.kiwiproject.elucidation.data.canary.job;

import static javax.ws.rs.client.Entity.json;

import org.kiwiproject.elucidation.client.helper.jersey.InboundHttpRequestTrackingFilter;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import java.util.List;
import java.util.Map;

@Slf4j
public class CrudDeviceCanary {

    private static final String BRAND = "brand";
    private static final String LOCATION = "location";
    private static final String DEVICE_TYPE_ID = "deviceTypeId";
    private static final String SERVICE_NAME = "canary-service";
    private final Client httpClient;

    public CrudDeviceCanary(Client httpClient) {
        this.httpClient = httpClient;
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
        var thermostatResponse = httpClient.target("http://thermostat:8080/thermostat/register")
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
                .post(json(Map.of("name", name, BRAND, "Nest", LOCATION, location, "currentTemp", 0.0)));

        if (thermostatResponse.getStatus() == 201) {
            var id = thermostatResponse.readEntity(new GenericType<Map<String, Long>>(){}).get("id");
            registerDevice(name, id, "THERMOSTAT");
        } else {
            LOG.warn("Unable to save thermostat. Status: {} Body: {}", thermostatResponse.getStatus(), thermostatResponse.readEntity(String.class));
        }
    }

    private int createAndRegisterLight(String name, String location) {
        var lightResponse = httpClient.target("http://light:8080/light/register")
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
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
        var doorbellResponse = httpClient.target("http://doorbell:8080/doorbell/register")
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
                .post(json(Map.of("name", "Front Doorbell", BRAND, "Ring")));

        if (doorbellResponse.getStatus() == 201) {
            var id = doorbellResponse.readEntity(new GenericType<Map<String, Long>>(){}).get("id");
            registerDevice("Front Doorbell", id, "DOORBELL");
        } else {
            LOG.warn("Unable to save doorbell. Status: {} Body: {}", doorbellResponse.getStatus(), doorbellResponse.readEntity(String.class));
        }
    }

    private void createAndRegisterAppliance() {
        var applianceResponse = httpClient.target("http://appliance:8080/appliance/register")
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
                .post(json(Map.of("name", "Coffee Machine", BRAND, "Kuerig", LOCATION, "Kitchen", "state", "OFF")));

        if (applianceResponse.getStatus() == 201) {
            var id = applianceResponse.readEntity(new GenericType<Map<String, Long>>(){}).get("id");
            registerDevice("Coffee Machine", id, "APPLIANCE");
        } else {
            LOG.warn("Unable to save appliance. Status: {} Body: {}", applianceResponse.getStatus(), applianceResponse.readEntity(String.class));
        }
    }

    private int registerDevice(String name, long id, String type) {
        var response = httpClient.target("http://home:8080/home/device/register")
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
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
        var deviceResponse = httpClient.target("http://home:8080/home/device")
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
                .get();

        if (deviceResponse.getStatus() == 200) {
            LOG.info("Retrieved all devices");
            var device = deviceResponse.readEntity(new GenericType<List<Map<String, Object>>>(){}).stream()
                    .filter(deviceMap -> deviceId == (int) deviceMap.get("id"))
                    .findFirst()
                    .orElseThrow();

            LOG.info("Found light to delete. {}", device.get(DEVICE_TYPE_ID));
            var lightResponse = httpClient.target("http://light:8080/light/{id}")
                    .resolveTemplate("id", device.get(DEVICE_TYPE_ID))
                    .request()
                    .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
                    .delete();

            if (lightResponse.getStatus() == 202) {
                LOG.info("Light has been deleted");
            } else {
                LOG.warn("Unable to delete light. Status: {} Body: {}", lightResponse.getStatus(), lightResponse.readEntity(String.class));
            }

            var deviceDeleteResponse = httpClient.target("http://home:8080/home/device/{id}")
                    .resolveTemplate("id", deviceId)
                    .request()
                    .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
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
