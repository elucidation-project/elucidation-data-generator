package org.kiwiproject.elucidation.data.light.jms;

import static org.kiwiproject.elucidation.data.light.App.SERVICE_NAME;
import static java.util.Objects.nonNull;
import static javax.ws.rs.client.Entity.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kiwiproject.elucidation.client.ElucidationClient;
import org.kiwiproject.elucidation.client.ElucidationRecorder;
import org.kiwiproject.elucidation.client.helper.jersey.InboundHttpRequestTrackingFilter;
import org.kiwiproject.elucidation.common.definition.JmsCommunicationDefinition;
import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;
import org.kiwiproject.elucidation.data.light.db.SmartLightDao;
import org.kiwiproject.elucidation.data.light.model.Event;
import org.kiwiproject.elucidation.data.light.model.SmartLight;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.ws.rs.client.Client;
import java.util.Optional;

@Slf4j
public class JmsConsumer implements MessageListener {

    private final SmartLightDao dao;
    private final ElucidationClient<Event> elucidationClient;
    private final ObjectMapper json;
    private final Client httpClient;

    public JmsConsumer(SmartLightDao dao, ElucidationRecorder recorder, ObjectMapper json, Client httpClient) {
        this.dao = dao;
        this.json = json;
        this.httpClient = httpClient;

        var communicationDef = new JmsCommunicationDefinition();
        this.elucidationClient = ElucidationClient.of(recorder, evt -> Optional.of(ConnectionEvent.builder()
                .communicationType(communicationDef.getCommunicationType())
                .connectionIdentifier(evt.getAction())
                .eventDirection(Direction.INBOUND)
                .serviceName("light-service")
                .observedAt(System.currentTimeMillis())
                .build()));
    }

    @SuppressWarnings("java:S2095")
    public void start() {
        try {
            var factory = new ActiveMQConnectionFactory("tcp://artemis:61616");
            var jmsContext = factory.createContext("elucidation", "password", JMSContext.AUTO_ACKNOWLEDGE);

            jmsContext.setClientID("light-service");

            var topic = jmsContext.createTopic("iotEvent");
            var consumer = jmsContext.createConsumer(topic);
            consumer.setMessageListener(this);
            LOG.info("Connection to Artemis is setup");
        } catch (Exception e) {
            LOG.error("Got an error", e);
        }
    }

    public void onMessage(Message message) {
        var txtMsg = (TextMessage) message;

        try {
            LOG.info("Got message: {}", txtMsg.getText());

            var evt = json.readValue(txtMsg.getText(), Event.class);

            if ("light".equalsIgnoreCase(evt.getAction())) {
                LOG.info("Message is for light, so processing");
                recordEvent(evt);

                dao.setColor(SmartLight.Color.valueOf((String) evt.getValue().get("color")), evt.getIotLookup());
                dao.setBrightness((Integer) evt.getValue().get("brightness"), evt.getIotLookup());

                var light = dao.findById(evt.getIotLookup());
                httpClient.target("http://home:8080/home/device/record/event/{type}/{name}")
                        .resolveTemplate("type", "LIGHT")
                        .resolveTemplate("name", light.orElseThrow().getName())
                        .request()
                        .header(InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER, SERVICE_NAME)
                        .put(json(""));
            }
        } catch (Exception e) {
            LOG.error("Problem reading message", e);
        }
    }

    private void recordEvent(Event evt) {
        elucidationClient.recordNewEvent(evt).whenComplete((result, exception) -> {
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
