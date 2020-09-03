package com.fortitudetec.elucidation.data.light;

import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.client.helper.dropwizard.EndpointTrackingListener;
import com.fortitudetec.elucidation.client.helper.jersey.InboundHttpRequestTrackingFilter;
import com.fortitudetec.elucidation.data.light.config.AppConfig;
import com.fortitudetec.elucidation.data.light.db.SmartLightDao;
import com.fortitudetec.elucidation.data.light.jms.JmsConsumer;
import com.fortitudetec.elucidation.data.light.resource.SmartLightResource;
import io.dropwizard.Application;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.ws.rs.client.ClientBuilder;
import java.util.concurrent.TimeUnit;

@Slf4j
public class App extends Application<AppConfig> {

    public static final String SERVICE_NAME = "light-service";

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
        var lightDao = jdbi.onDemand(SmartLightDao.class);

        var eventRecorder = setupEventRecorder();
        env.jersey().register(new SmartLightResource(lightDao));
        startConsumer(lightDao, env, eventRecorder);

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
        var jdbi = new JdbiFactory().build(env, config.getDataSourceFactory(), "Light-Service-Data-Source");
        jdbi.installPlugin(new SqlObjectPlugin());
        return jdbi;
    }

    private ElucidationRecorder setupEventRecorder() {
        // When using docker compose, elucidation will resolve
        return new ElucidationRecorder("http://elucidation:8080");
    }

    private void startConsumer(SmartLightDao lightDao, Environment env, ElucidationRecorder eventRecorder) {
        var executor = env.lifecycle().scheduledExecutorService("jms").build();

        var httpClient = ClientBuilder.newClient();
        executor.schedule(() -> {
            var jmsConsumer = new JmsConsumer(lightDao, eventRecorder, env.getObjectMapper(), httpClient);
            jmsConsumer.start();
        }, 30, TimeUnit.SECONDS);
    }
}

