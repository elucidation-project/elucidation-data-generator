package com.fortitudetec.elucidation.data.canary;

import com.fortitudetec.elucidation.client.ElucidationClient;
import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.client.helper.dropwizard.EndpointTrackingListener;
import com.fortitudetec.elucidation.data.canary.config.AppConfig;
import com.fortitudetec.elucidation.data.canary.job.RunTestsJob;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.ClientBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class App extends Application<AppConfig> {

    public static void main(String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    public void run(AppConfig config, Environment env) {
        var eventRecorder = setupEventRecorder();
        var httpClient = ClientBuilder.newClient();

        var executor = env.lifecycle().scheduledExecutorService("Canary-Test-Runner").build();
        executor.schedule(new RunTestsJob(httpClient, eventRecorder), 1, TimeUnit.MINUTES);

        env.jersey().register(new EndpointTrackingListener<String>(
                env.jersey().getResourceConfig(),
                "canary-service",
                ElucidationClient.of(eventRecorder, info -> Optional.empty())));
    }

    private ElucidationRecorder setupEventRecorder() {
        // When using docker compose, elucidation will resolve
        return new ElucidationRecorder("http://elucidation:8080");
    }
}

