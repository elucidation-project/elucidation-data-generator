package com.fortitudetec.elucidation.data.appliance.jms;

import com.fortitudetec.elucidation.data.appliance.db.ApplianceDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.ws.rs.client.Client;

@Slf4j
public class JmsConsumer implements MessageListener {

    private final ApplianceDao dao;
    private final Client client;

    public JmsConsumer(ApplianceDao dao, Client client) {
        this.dao = dao;
        this.client = client;
    }

    public void start() {
        try (var factory = new ActiveMQConnectionFactory("tcp://artemis:61616");
             var jmsContext = factory.createContext("elucidation", "password", JMSContext.AUTO_ACKNOWLEDGE)) {

            jmsContext.setClientID("appliance-service");

            var topic = jmsContext.createTopic("iotEvent");
            try (var consumer = jmsContext.createSharedConsumer(topic, "appliance-service-consumer-1")) {
                consumer.setMessageListener(this);
                jmsContext.start();
                LOG.info("Connection to Artemis is setup");
            }
        }
    }

    public void onMessage(Message message) {
        var txtMsg = (TextMessage) message;

        try {
            // TODO: process message
            LOG.info("Got message: {}", txtMsg.getText());
        } catch (JMSException e) {
            LOG.error("Problem reading message", e);
        }
    }
}
