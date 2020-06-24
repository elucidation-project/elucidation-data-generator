package com.fortitudetec.elucidation.data.canary.job;

import static javax.ws.rs.client.Entity.json;

import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.common.definition.HttpCommunicationDefinition;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class DoorbellWorkflowCanary {

    private final Client httpClient;
    private final ElucidationClient<String> client;

    public DoorbellWorkflowCanary(Client httpClient, ElucidationRecorder eventRecorder) {
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
        LOG.info("********************************************************");
        LOG.info("* Running canary test to perform the Doorbell workflow *");
        LOG.info("********************************************************");

        // Create workflow and send to home service
        var workflowData = Map.of(
                "name", "Doorbell",
                "stepJson", readWorkflowJson("doorbell_workflow_steps.json")
        );

        createWorkflow(workflowData);

        // Trigger workflow (call to doorbell to simulate it being pressed)
        triggerWorkflow();
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

    private void createWorkflow(Map<String, String> workflowData) {
        client.recordNewEvent("POST /home/workflow");
        var workflowResponse = httpClient.target("http://home:8080/home/workflow")
                .request()
                .post(json(workflowData));

        if (workflowResponse.getStatus() == 201) {
            var id = workflowResponse.readEntity(new GenericType<Map<String, Integer>>(){}).get("id");
            LOG.info("Workflow created with id {}", id);
        } else {
            LOG.warn("Unable to save workflow. Status: {} Body: {}", workflowResponse.getStatus(), workflowResponse.readEntity(String.class));
        }
    }

    private void triggerWorkflow() {
        var doorbellId = findDoorbell();

        client.recordNewEvent("POST /doorbell/{id}/ring");
        var workflowResponse = httpClient.target("http://doorbell:8080/doorbell/{id}/ring")
                .resolveTemplate("id", doorbellId)
                .request()
                .post(json(""));

        if (workflowResponse.getStatus() == 202) {
            LOG.info("Doorbell {} pressed", doorbellId);
        } else {
            LOG.warn("Unable to ring doorbell. Status: {} Body: {}", workflowResponse.getStatus(), workflowResponse.readEntity(String.class));
        }
    }

    private int findDoorbell() {
        client.recordNewEvent("GET /doorbell");
        var response = httpClient.target("http://doorbell:8080/doorbell")
                .request()
                .get();

        if (response.getStatus() == 200) {
            return (int) response.readEntity(new GenericType<List<Map<String, Object>>>(){}).get(0).get("id");
        } else {
            LOG.warn("Unable to find doorbell. Status: {} Body: {}", response.getStatus(), response.readEntity(String.class));
        }

        return -1;
    }
}
