package com.fortitudetec.elucidation.data.canary.job;

import static javax.ws.rs.client.Entity.json;

import com.fortitudetec.elucidation.client.helper.jersey.InboundHttpRequestTrackingFilter;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class GoodMorningWorkflowCanary {

    private static final String SERVICE_NAME = "canary-service";
    private final Client httpClient;

    public GoodMorningWorkflowCanary(Client httpClient) {
        this.httpClient = httpClient;
    }

    public void runCanaryTest() {
        LOG.info("************************************************************");
        LOG.info("* Running canary test to perform the Good Morning workflow *");
        LOG.info("************************************************************");

        // Setup dummy device
        createAndRegisterCamera();

        // Create workflow and send to home service
        var workflowData = Map.of(
                "name", "Good Morning",
                "stepJson", readWorkflowJson()
        );

        var workflowId = createWorkflow(workflowData);

        // Trigger workflow (call to home to simulate start of day)
        triggerWorkflow(workflowId);
    }

    @SuppressWarnings("UnstableApiUsage")
    private String readWorkflowJson() {
        var url = Resources.getResource("good_morning_workflow_steps.json");
        try {
            return Resources.toString(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Unable to read workflow file good_morning_workflow_steps.json. Returning empty array.");
        }

        return "[]";
    }

    private int createWorkflow(Map<String, String> workflowData) {
        var workflowResponse = httpClient.target("http://home:8080/home/workflow")
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
                .post(json(workflowData));

        if (workflowResponse.getStatus() == 201) {
            var id = workflowResponse.readEntity(new GenericType<Map<String, Integer>>(){}).get("id");
            LOG.info("Workflow created with id {}", id);
            return id;
        } else {
            LOG.warn("Unable to save workflow. Status: {} Body: {}", workflowResponse.getStatus(), workflowResponse.readEntity(String.class));
        }

        return -1;
    }

    private void triggerWorkflow(int workflowId) {
        var workflowResponse = httpClient.target("http://home:8080/home/workflow/trigger/byId/{id}")
                .resolveTemplate("id", workflowId)
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
                .put(json(""));

        if (workflowResponse.getStatus() == 202) {
            LOG.info("Workflow {} triggered", workflowId);
        } else {
            LOG.warn("Unable to trigger workflow. Status: {} Body: {}", workflowResponse.getStatus(), workflowResponse.readEntity(String.class));
        }
    }

    private void createAndRegisterCamera() {
        var response = httpClient.target("http://home:8080/home/device/register")
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
                .post(json(Map.of("name", "Garage Camera", "deviceType", "CAMERA", "deviceTypeId", 1)));

        if (response.getStatus() == 201) {
            var deviceId = response.readEntity(new GenericType<Map<String, Integer>>(){}).get("id");

            LOG.info("CAMERA Garage Camera created with id: {}", deviceId);
        } else {
            LOG.warn("Unable to save device. Status: {} Body: {}", response.getStatus(), response.readEntity(String.class));
        }
    }
}
