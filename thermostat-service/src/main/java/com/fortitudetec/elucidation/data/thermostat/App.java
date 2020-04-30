package com.fortitudetec.elucidation.data.thermostat;

import com.fortitudetec.elucidation.data.thermostat.config.AppConfig;
import com.fortitudetec.elucidation.data.thermostat.db.ThermostatDao;
import com.fortitudetec.elucidation.data.thermostat.resource.ThermostatResource;
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
		var thermostatDao = jdbi.onDemand(ThermostatDao.class);

		env.jersey().register(new ThermostatResource(thermostatDao));
	}

	private Jdbi setupJdbi(AppConfig config, Environment env) {
		var jdbi = new JdbiFactory().build(env, config.getDataSourceFactory(), "Thermostat-Service-Data-Source");
		jdbi.installPlugin(new SqlObjectPlugin());
		return jdbi;
	}
}
