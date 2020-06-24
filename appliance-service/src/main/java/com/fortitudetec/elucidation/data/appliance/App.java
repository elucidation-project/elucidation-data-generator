package com.fortitudetec.elucidation.data.appliance;

import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.client.helper.dropwizard.EndpointTrackingListener;
import com.fortitudetec.elucidation.data.appliance.config.AppConfig;
import com.fortitudetec.elucidation.data.appliance.db.ApplianceDao;
import com.fortitudetec.elucidation.data.appliance.jms.JmsConsumer;
import com.fortitudetec.elucidation.data.appliance.resource.ApplianceResource;
import io.dropwizard.Application;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class App extends Application<AppConfig> {

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
		var applianceDao = jdbi.onDemand(ApplianceDao.class);

		var eventRecorder = setupEventRecorder();
		env.jersey().register(new ApplianceResource(applianceDao, eventRecorder));
		startConsumer(applianceDao, env, eventRecorder);

		env.jersey().register(new EndpointTrackingListener<String>(
				env.jersey().getResourceConfig(),
				"appliance-service",
				ElucidationClient.of(eventRecorder, info -> Optional.empty())));
	}

	private Jdbi setupJdbi(AppConfig config, Environment env) {
		var jdbi = new JdbiFactory().build(env, config.getDataSourceFactory(), "Appliance-Service-Data-Source");
		jdbi.installPlugin(new SqlObjectPlugin());
		return jdbi;
	}

	private ElucidationRecorder setupEventRecorder() {
		// When using docker compose, elucidation will resolve
		return new ElucidationRecorder("http://elucidation:8080");
	}

	private void startConsumer(ApplianceDao applianceDao, Environment env, ElucidationRecorder eventRecorder) {
		var executor = env.lifecycle().scheduledExecutorService("jms").build();

		executor.schedule(() -> {
			var jmsConsumer = new JmsConsumer(applianceDao, eventRecorder, env.getObjectMapper());
			jmsConsumer.start();
		}, 30, TimeUnit.SECONDS);
	}
}

