package com.fortitudetec.elucidation.data.home.service;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.common.definition.JmsCommunicationDefinition;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.Direction;
import com.fortitudetec.elucidation.data.home.db.DeviceDao;
import com.fortitudetec.elucidation.data.home.model.Device;
import com.fortitudetec.elucidation.data.home.model.Event;
import com.fortitudetec.elucidation.data.home.model.Workflow;
import com.fortitudetec.elucidation.data.home.model.WorkflowStep;
import lombok.extern.slf4j.Slf4j;

import javax.jms.JMSProducer;
import javax.jms.Topic;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class WorkflowService {

    private final JMSProducer producer;
    private final Topic topic;
    private final DeviceDao deviceDao;
    private final ObjectMapper json;
    private final ElucidationClient<Event> client;

    public WorkflowService(JMSProducer producer, Topic topic, DeviceDao deviceDao, ObjectMapper json, ElucidationRecorder recorder) {
        this.producer = producer;
        this.topic = topic;
        this.deviceDao = deviceDao;
        this.json = json;

        var communicationDef = new JmsCommunicationDefinition();
        this.client = ElucidationClient.of(recorder, evt -> Optional.of(ConnectionEvent.builder()
                .communicationType(communicationDef.getCommunicationType())
                .connectionIdentifier(evt.getAction())
                .eventDirection(Direction.OUTBOUND)
                .serviceName("home-service")
                .observedAt(System.currentTimeMillis())
                .build()));
    }

    public void runWorkflow(Workflow workflow) {
        LOG.info("Running workflow {}", workflow.getName());

        try {
            var steps = json.readValue(workflow.getStepJson(), new TypeReference<List<WorkflowStep>>() {});

            steps.forEach(step -> {
                var optionalDevice = deviceDao.findByNameAndType(step.getDevice().getName(), step.getDevice().getDeviceType());
                optionalDevice.ifPresent(device -> sendEvent(step, device));

                if (step.getNextStepDelayInSeconds() > 0) {
                    LOG.info("Waiting for {} seconds to trigger the next step", step.getNextStepDelayInSeconds());
                    try {
                        Thread.sleep(step.getNextStepDelayInSeconds() * 1_000L);
                    } catch (InterruptedException e) {
                        LOG.warn("Sleep interrupted", e);
                        Thread.currentThread().interrupt();
                    }
                }
            });

        } catch (Exception e) {
            LOG.error("Problem running workflow {}", workflow.getName(), e);
        }
    }

    private void sendEvent(WorkflowStep step, Device device) {
        LOG.info("Sending workflow event for '{}'", step.getDescription());

        var event = Event.builder()
                .uuid(UUID.randomUUID().toString())
                .action(step.getEventAction())
                .iotLookup(device.getId())
                .value(step.getEventInfo())
                .build();

        try {
            producer.send(topic, json.writeValueAsString(event));
            recordEvent(event);
        } catch (Exception e) {
            LOG.error("Problem creating json", e);
        }
    }

    private void recordEvent(Event evt) {
        client.recordNewEvent(evt).whenComplete((result, exception) -> {
            if (nonNull(exception)) {
                LOG.error("An error occurred recording an event.", exception);
                return;
            }

            switch (result.getStatus()) {
                case SUCCESS:
                    LOG.info("Successfully recorded event to Elucidation");
                    break;
                case SKIPPED:
                    LOG.info("Recording was skipped. Shouldn't happen here");
                    break;
                case ERROR:
                    LOG.error("Had a problem recording event. Error: {} Exception: {}", result.getErrorMessage(), result.getException());
            }
        });
    }
}
