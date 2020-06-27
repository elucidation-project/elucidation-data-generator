package com.fortitudetec.elucidation.data.doorbell;

import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.client.helper.dropwizard.EndpointTrackingListener;
import com.fortitudetec.elucidation.client.helper.jersey.InboundHttpRequestTrackingFilter;
import com.fortitudetec.elucidation.data.doorbell.config.AppConfig;
import com.fortitudetec.elucidation.data.doorbell.db.DoorbellDao;
import com.fortitudetec.elucidation.data.doorbell.resource.DoorbellResource;
import com.fortitudetec.elucidation.data.doorbell.service.DoorbellService;
import io.dropwizard.Application;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

@Slf4j
public class App extends Application<AppConfig> {

	public static final String SERVICE_NAME = "doorbell-service";

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
		var doorbellDao = jdbi.onDemand(DoorbellDao.class);

		var eventRecorder = setupEventRecorder();
		var doorbellService = new DoorbellService();
		env.jersey().register(new DoorbellResource(doorbellDao, doorbellService));

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
		var jdbi = new JdbiFactory().build(env, config.getDataSourceFactory(), "Doorbell-Service-Data-Source");
		jdbi.installPlugin(new SqlObjectPlugin());
		return jdbi;
	}

	private ElucidationRecorder setupEventRecorder() {
		// When using docker compose, elucidation will resolve
		return new ElucidationRecorder("http://elucidation:8080");
	}
}

