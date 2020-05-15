package com.fortitudetec.elucidation.data.canary.job;

import com.fortitudetec.elucidation.client.ElucidationEventRecorder;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;

@Slf4j
public class RunTestsJob implements Runnable {

    private final Client httpClient;
    private final ElucidationEventRecorder eventRecorder;

    public RunTestsJob(Client httpClient, ElucidationEventRecorder eventRecorder) {
        this.httpClient = httpClient;
        this.eventRecorder = eventRecorder;
    }

    public void run() {
        //TODO: Build test runners
        LOG.info("Running data generation tests!");
    }
}
