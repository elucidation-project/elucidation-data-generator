package org.kiwiproject.elucidation.data.thermostat;

import org.kiwiproject.elucidation.client.ElucidationRecorder;
import org.kiwiproject.elucidation.client.helper.dropwizard.EndpointTrackingListener;
import org.kiwiproject.elucidation.client.helper.jersey.InboundHttpRequestTrackingFilter;
import org.kiwiproject.elucidation.data.thermostat.config.AppConfig;
import org.kiwiproject.elucidation.data.thermostat.db.ThermostatDao;
import org.kiwiproject.elucidation.data.thermostat.jms.JmsConsumer;
import org.kiwiproject.elucidation.data.thermostat.resource.ThermostatResource;
import io.dropwizard.Application;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.util.concurrent.TimeUnit;

@Slf4j
public class App extends Application<AppConfig> {

	private static final String SERVICE_NAME = "thermostat-service";

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
		var thermostatDao = jdbi.onDemand(ThermostatDao.class);

		var eventRecorder = setupEventRecorder();
		env.jersey().register(new ThermostatResource(thermostatDao));
		startConsumer(thermostatDao, env, eventRecorder);

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
		var jdbi = new JdbiFactory().build(env, config.getDataSourceFactory(), "Thermostat-Service-Data-Source");
		jdbi.installPlugin(new SqlObjectPlugin());
		return jdbi;
	}

	private ElucidationRecorder setupEventRecorder() {
		// When using docker compose, elucidation will resolve
		return new ElucidationRecorder("http://elucidation:8080");
	}

	private void startConsumer(ThermostatDao thermostatDao, Environment env, ElucidationRecorder eventRecorder) {
		var executor = env.lifecycle().scheduledExecutorService("jms").build();

		executor.schedule(() -> {
			var jmsConsumer = new JmsConsumer(thermostatDao, eventRecorder, env.getObjectMapper());
			jmsConsumer.start();
		}, 30, TimeUnit.SECONDS);
	}
}

