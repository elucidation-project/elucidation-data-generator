package com.fortitudetec.elucidation.data.home;

import static java.util.Objects.nonNull;

import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.client.helper.dropwizard.EndpointTrackingListener;
import com.fortitudetec.elucidation.client.helper.jersey.InboundHttpRequestTrackingFilter;
import com.fortitudetec.elucidation.data.home.config.AppConfig;
import com.fortitudetec.elucidation.data.home.db.DeviceDao;
import com.fortitudetec.elucidation.data.home.db.WorkflowDao;
import com.fortitudetec.elucidation.data.home.resource.DeviceResource;
import com.fortitudetec.elucidation.data.home.resource.WorkflowResource;
import com.fortitudetec.elucidation.data.home.service.WorkflowService;
import io.dropwizard.Application;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.jms.JMSContext;
import java.util.concurrent.TimeUnit;

@Slf4j
public class App extends Application<AppConfig> {

    private static final String SERVICE_NAME = "home-service";

    public static void main(String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    public void initialize(Bootstrap<AppConfig> bootstrap) {
        bootstrap.addBundle(new MigrationsBundle<>() {

            @Override
            public PooledDataSourceFactory getDataSourceFactory(AppConfig configuration) {
                return configuration.getDataSourceFactory();
            }
        });
    }

    @Override
    public void run(AppConfig config, Environment env) {
        var jdbi = setupJdbi(config, env);

        var deviceDao = jdbi.onDemand(DeviceDao.class);
        var workflowDao = jdbi.onDemand(WorkflowDao.class);

        var jmsContext = startContext(env, config);

        var eventRecorder = setupEventRecorder();

        WorkflowService workflowService;
        if (nonNull(jmsContext)) {
            var producer = jmsContext.createProducer();
            var topic = jmsContext.createTopic("iotEvent");
            LOG.info("Producer to Artemis is setup");
            workflowService = new WorkflowService(producer, topic, deviceDao, env.getObjectMapper(), eventRecorder);
         } else {
            workflowService = new WorkflowService(null, null, deviceDao, env.getObjectMapper(), eventRecorder);
        }

        env.jersey().register(new DeviceResource(deviceDao));
        env.jersey().register(new WorkflowResource(workflowDao, workflowService));

        env.jersey().register(new EndpointTrackingListener(
                env.jersey().getResourceConfig(),
                SERVICE_NAME,
                eventRecorder));

        env.jersey().register(new InboundHttpRequestTrackingFilter(
                SERVICE_NAME,
                eventRecorder,
                InboundHttpRequestTrackingFilter.ELUCIDATION_ORIGINATING_SERVICE_HEADER));
    }

    private Jdbi setupJdbi(AppConfig config, Environment env) {
        var jdbi = new JdbiFactory().build(env, config.getDataSourceFactory(), "Home-Service-Data-Source");
        jdbi.installPlugin(new SqlObjectPlugin());
        return jdbi;
    }

    private ElucidationRecorder setupEventRecorder() {
        // When using docker compose, elucidation will resolve
        return new ElucidationRecorder("http://elucidation:8080");
    }

    private JMSContext startContext(Environment env, AppConfig config) {
        var executor = env.lifecycle().scheduledExecutorService("jms").build();

        var future = executor.schedule(() -> createContext(config), 30, TimeUnit.SECONDS);

        try {
            return future.get();
        } catch (Exception e) {
            LOG.error("Error", e);
            return null;
        }
    }

    @SuppressWarnings("java:S2095")
    private JMSContext createContext(AppConfig config) {
        var factory = new ActiveMQConnectionFactory(config.getArtemisUrl());
        var jmsContext = factory.createContext("elucidation", "password", JMSContext.AUTO_ACKNOWLEDGE);

        jmsContext.setClientID(SERVICE_NAME);
        return jmsContext;
    }
}

