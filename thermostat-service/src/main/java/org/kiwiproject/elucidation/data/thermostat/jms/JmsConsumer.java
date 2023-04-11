package org.kiwiproject.elucidation.data.thermostat.jms;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kiwiproject.elucidation.client.ElucidationClient;
import org.kiwiproject.elucidation.client.ElucidationRecorder;
import org.kiwiproject.elucidation.common.definition.JmsCommunicationDefinition;
import org.kiwiproject.elucidation.common.model.ConnectionEvent;
import org.kiwiproject.elucidation.common.model.Direction;
import org.kiwiproject.elucidation.data.thermostat.db.ThermostatDao;
import org.kiwiproject.elucidation.data.thermostat.model.Event;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.Optional;

@Slf4j
public class JmsConsumer implements MessageListener {

    private final ThermostatDao dao;
    private final ElucidationClient<Event> elucidationClient;
    private final ObjectMapper json;

    public JmsConsumer(ThermostatDao dao, ElucidationRecorder recorder, ObjectMapper json) {
        this.dao = dao;
        this.json = json;

        var communicationDef = new JmsCommunicationDefinition();
        this.elucidationClient = ElucidationClient.of(recorder, evt -> Optional.of(ConnectionEvent.builder()
                .communicationType(communicationDef.getCommunicationType())
                .connectionIdentifier(evt.getAction())
                .eventDirection(Direction.INBOUND)
                .serviceName("thermostat-service")
                .observedAt(System.currentTimeMillis())
                .build()));
    }

    @SuppressWarnings("java:S2095")
    public void start() {
        try {
            var factory = new ActiveMQConnectionFactory("tcp://artemis:61616");
            var jmsContext = factory.createContext("elucidation", "password", JMSContext.AUTO_ACKNOWLEDGE);

            jmsContext.setClientID("thermostat-service");

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

            if ("temp".equalsIgnoreCase(evt.getAction())) {
                LOG.info("Message is for thermostat, so processing");
                recordEvent(evt);

                dao.setCurrentTemp((Double) evt.getValue().get("temp"), evt.getIotLookup());
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
