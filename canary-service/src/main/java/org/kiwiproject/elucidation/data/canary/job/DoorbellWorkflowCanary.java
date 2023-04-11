package org.kiwiproject.elucidation.data.canary.job;

import static javax.ws.rs.client.Entity.json;

import org.kiwiproject.elucidation.client.helper.jersey.InboundHttpRequestTrackingFilter;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
public class DoorbellWorkflowCanary {

    private static final String SERVICE_NAME = "canary-service";
    private final Client httpClient;

    public DoorbellWorkflowCanary(Client httpClient) {
        this.httpClient = httpClient;
    }

    public void runCanaryTest() {
        LOG.info("********************************************************");
        LOG.info("* Running canary test to perform the Doorbell workflow *");
        LOG.info("********************************************************");

        // Create workflow and send to home service
        var workflowData = Map.of(
                "name", "Doorbell",
                "stepJson", readWorkflowJson()
        );

        createWorkflow(workflowData);

        // Trigger workflow (call to doorbell to simulate it being pressed)
        triggerWorkflow();
    }

    @SuppressWarnings("UnstableApiUsage")
    private String readWorkflowJson() {
        var url = Resources.getResource("doorbell_workflow_steps.json");
        try {
            return Resources.toString(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Unable to read workflow file doorbell_workflow_steps.json. Returning empty array.");
        }

        return "[]";
    }

    private void createWorkflow(Map<String, String> workflowData) {
        var workflowResponse = httpClient.target("http://home:8080/home/workflow")
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
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

        var workflowResponse = httpClient.target("http://doorbell:8080/doorbell/{id}/ring")
                .resolveTemplate("id", doorbellId)
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
                .post(json(""));

        if (workflowResponse.getStatus() == 202) {
            LOG.info("Doorbell {} pressed", doorbellId);
        } else {
            LOG.warn("Unable to ring doorbell. Status: {} Body: {}", workflowResponse.getStatus(), workflowResponse.readEntity(String.class));
        }
    }

    private int findDoorbell() {
        var response = httpClient.target("http://doorbell:8080/doorbell")
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
                .get();

        if (response.getStatus() == 200) {
            return (int) response.readEntity(new GenericType<List<Map<String, Object>>>(){}).get(0).get("id");
        } else {
            LOG.warn("Unable to find doorbell. Status: {} Body: {}", response.getStatus(), response.readEntity(String.class));
        }

        return -1;
    }
}
