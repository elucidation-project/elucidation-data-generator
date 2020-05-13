package com.fortitudetec.elucidation.data.home;

import com.fortitudetec.elucidation.client.ElucidationEventRecorder;
import com.fortitudetec.elucidation.data.home.config.AppConfig;
import com.fortitudetec.elucidation.data.home.db.DeviceDao;
import com.fortitudetec.elucidation.data.home.db.WorkflowDao;
import com.fortitudetec.elucidation.data.home.resource.DeviceResource;
import com.fortitudetec.elucidation.data.home.resource.WorkflowResource;
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

		var eventRecorder = setupEventRecorder();
		env.jersey().register(new DeviceResource(deviceDao, eventRecorder));
		env.jersey().register(new WorkflowResource(workflowDao, eventRecorder));
	}

	private Jdbi setupJdbi(AppConfig config, Environment env) {
		var jdbi = new JdbiFactory().build(env, config.getDataSourceFactory(), "Home-Service-Data-Source");
		jdbi.installPlugin(new SqlObjectPlugin());
		return jdbi;
	}

	private ElucidationEventRecorder setupEventRecorder() {
		// When using docker compose, elucidation will resolve
		return new ElucidationEventRecorder("http://elucidation:8080");
	}
}

