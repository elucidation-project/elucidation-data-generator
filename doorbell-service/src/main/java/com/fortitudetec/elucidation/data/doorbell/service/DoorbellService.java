package com.fortitudetec.elucidation.data.doorbell.service;

import static javax.ws.rs.client.Entity.json;

import com.fortitudetec.elucidation.client.helper.jersey.InboundHttpRequestTrackingFilter;
import com.fortitudetec.elucidation.data.doorbell.App;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

@Slf4j
public class DoorbellService {

    private final Client httpClient;

    public DoorbellService() {
        this.httpClient = ClientBuilder.newClient();
    }

    public void ringDoorbell() {
        var workflowResponse = httpClient.target("http://home:8080/home/workflow/trigger/byName/{name}")
                .resolveTemplate("name", "Doorbell")
                .request()
                .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, App.SERVICE_NAME)
                .put(json(""));

        if (workflowResponse.getStatus() == 202) {
            LOG.info("Workflow 'Doorbell' triggered");
        } else {
            LOG.warn("Unable to trigger workflow. Status: {} Body: {}", workflowResponse.getStatus(), workflowResponse.readEntity(String.class));
        }
    }
}
