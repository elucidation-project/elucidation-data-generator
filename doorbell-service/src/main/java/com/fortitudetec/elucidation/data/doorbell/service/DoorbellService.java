package com.fortitudetec.elucidation.data.doorbell.service;

import static javax.ws.rs.client.Entity.json;

import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.ElucidationEventRecorder;
import com.fortitudetec.elucidation.common.definition.HttpCommunicationDefinition;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.Optional;

@Slf4j
public class DoorbellService {

    private final ElucidationClient<String> outboundClient;
    private final Client httpClient;

    public DoorbellService(ElucidationEventRecorder recorder) {
        var communicationDef = new HttpCommunicationDefinition();

        this.outboundClient = ElucidationClient.of(recorder, identifier -> Optional.of(ConnectionEvent.builder()
                .communicationType(communicationDef.getCommunicationType())
                .connectionIdentifier(identifier)
                .eventDirection(Direction.OUTBOUND)
                .serviceName("doorbell-service")
                .observedAt(System.currentTimeMillis())
                .build()));

        this.httpClient = ClientBuilder.newClient();
    }

    public void ringDoorbell() {
        outboundClient.recordNewEvent("PUT /home/workflow/trigger/byName/{name}");
        var workflowResponse = httpClient.target("http://home:8080/home/workflow/trigger/byName/{name}")
                .resolveTemplate("name", "Doorbell")
                .request()
                .put(json(""));

        if (workflowResponse.getStatus() == 202) {
            LOG.info("Workflow 'Doorbell' triggered");
        } else {
            LOG.warn("Unable to trigger workflow. Status: {} Body: {}", workflowResponse.getStatus(), workflowResponse.readEntity(String.class));
        }
    }
}
