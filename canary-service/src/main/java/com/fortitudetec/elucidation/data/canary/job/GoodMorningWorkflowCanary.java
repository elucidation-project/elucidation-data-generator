package com.fortitudetec.elucidation.data.canary.job;

import static javax.ws.rs.client.Entity.json;

import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.ElucidationEventRecorder;
import com.fortitudetec.elucidation.common.definition.HttpCommunicationDefinition;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class GoodMorningWorkflowCanary {

    private final Client httpClient;
    private final ElucidationClient<String> client;

    public GoodMorningWorkflowCanary(Client httpClient, ElucidationEventRecorder eventRecorder) {
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
        LOG.info("Running canary test to perform the Good Morning workflow");

        // Setup dummy device
        createAndRegisterCamera();

        // Create workflow and send to home service
        var workflowData = Map.of(
                "name", "Good Morning",
                "stepJson", readWorkflowJson("good_morning_workflow_steps.json")
        );

        var workflowId = createWorkflow(workflowData);

        // Trigger workflow (call to home to simulate
        triggerWorkflow(workflowId);
    }

    @SuppressWarnings("UnstableApiUsage")
    private String readWorkflowJson(String workflowName) {
        var url = Resources.getResource(workflowName);
        try {
            return Resources.toString(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Unable to read workflow file {}. Returning empty array.", workflowName);
        }

        return "[]";
    }

    private int createWorkflow(Map<String, String> workflowData) {
        client.recordNewEvent("POST /home/workflow");
        var workflowResponse = httpClient.target("http://home:8080/home/workflow")
                .request()
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
        client.recordNewEvent("PUT /home/workflow/{id}");
        var workflowResponse = httpClient.target("http://home:8080/home/workflow/{id}")
                .resolveTemplate("id", workflowId)
                .request()
                .put(json(""));

        if (workflowResponse.getStatus() == 202) {
            LOG.info("Workflow {} triggered", workflowId);
        } else {
            LOG.warn("Unable to trigger workflow. Status: {} Body: {}", workflowResponse.getStatus(), workflowResponse.readEntity(String.class));
        }
    }

    private void createAndRegisterCamera() {
        client.recordNewEvent("POST /home/device/register");
        var response = httpClient.target("http://home:8080/home/device/register")
                .request()
                .post(json(Map.of("name", "Garage Camera", "deviceType", "CAMERA", "deviceTypeId", 1)));

        if (response.getStatus() == 201) {
            var deviceId = response.readEntity(new GenericType<Map<String, Integer>>(){}).get("id");

            LOG.info("CAMERA Garage Camera created with id: {}", deviceId);
        } else {
            LOG.warn("Unable to save device. Status: {} Body: {}", response.getStatus(), response.readEntity(String.class));
        }
    }
}
