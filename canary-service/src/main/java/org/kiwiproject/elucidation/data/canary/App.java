package org.kiwiproject.elucidation.data.canary;

import org.kiwiproject.elucidation.data.canary.config.AppConfig;
import org.kiwiproject.elucidation.data.canary.job.RunTestsJob;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.ClientBuilder;
import java.util.concurrent.TimeUnit;

@Slf4j
public class App extends Application<AppConfig> {

    public static void main(String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    public void run(AppConfig config, Environment env) {
        var httpClient = ClientBuilder.newClient();

        var executor = env.lifecycle().scheduledExecutorService("Canary-Test-Runner").build();
        executor.schedule(new RunTestsJob(httpClient), 1, TimeUnit.MINUTES);
    }

}

