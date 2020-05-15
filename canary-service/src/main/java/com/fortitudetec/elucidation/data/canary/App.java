package com.fortitudetec.elucidation.data.canary;

import com.fortitudetec.elucidation.client.ElucidationEventRecorder;
import com.fortitudetec.elucidation.data.canary.config.AppConfig;
import com.fortitudetec.elucidation.data.canary.job.RunTestsJob;
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
        var eventRecorder = setupEventRecorder();
        var httpClient = ClientBuilder.newClient();

        var executor = env.lifecycle().scheduledExecutorService("Canary-Test-Runner").build();
        executor.schedule(new RunTestsJob(httpClient, eventRecorder), 1, TimeUnit.MINUTES);
    }

    private ElucidationEventRecorder setupEventRecorder() {
        // When using docker compose, elucidation will resolve
        return new ElucidationEventRecorder("http://elucidation:8080");
    }
}

