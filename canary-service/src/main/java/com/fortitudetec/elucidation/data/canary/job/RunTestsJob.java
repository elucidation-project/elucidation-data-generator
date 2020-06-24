package com.fortitudetec.elucidation.data.canary.job;

import static java.lang.String.format;

import com.fortitudetec.elucidation.client.ElucidationRecorder;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
import com.fortitudetec.elucidation.common.model.TrackedConnectionIdentifier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class RunTestsJob implements Runnable {

    private static final String[] EVENT_CSV_HEADERS = { "id", "serviceName", "eventDirection", "communicationType", "connectionIdentifier", "observedAt" };
    private static final String[] TRACK_CSV_HEADERS = { "id", "serviceName", "communicationType", "connectionIdentifier" };
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    private final Client httpClient;
    private final ElucidationRecorder eventRecorder;

    public RunTestsJob(Client httpClient, ElucidationRecorder eventRecorder) {
        this.httpClient = httpClient;
        this.eventRecorder = eventRecorder;
    }

    public void run() {
        LOG.info("Running data generation tests!");
        var now = LocalDateTime.now();

        try {
            new CrudDeviceCanary(httpClient, eventRecorder).runCanaryTest();

            // NOTE: These tests uses devices that were set up in the test above, so if
            // that changes, then this test might need an adjustment.
            new GoodMorningWorkflowCanary(httpClient, eventRecorder).runCanaryTest();
            new DoorbellWorkflowCanary(httpClient, eventRecorder).runCanaryTest();

        } catch (Exception e) {
            LOG.error("Test job threw an error", e);
        }

        try {
            // Need to let all the async stuff go through
            Thread.sleep(5000);

            writeOutElucidationEvents(now);
            writeOutTrackedIdentifiers(now);

            LOG.info("*********************************************");
            LOG.info("*   ELUCIDATION DATA HAS BEEN GENERATED!!   *");
            LOG.info("*    Data can be found in ./export_data     *");
            LOG.info("*  You may exit the system by typing CTL-C  *");
            LOG.info("*********************************************");
        } catch (Exception e) {
            LOG.error("Error writing events", e);
        }
    }

    private void writeOutElucidationEvents(LocalDateTime timeToPullFrom) {
        var dateStr = DATE_TIME_FORMATTER.format(timeToPullFrom);
        var fileName = format("elucidation-events-%s.csv", dateStr);

        LOG.info("Writing out events to ./export_data/{}", fileName);

        var response = httpClient.target("http://elucidation:8080/elucidate/events")
                .queryParam("since", timeToPullFrom.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .request()
                .get();

        if (response.getStatus() == 200) {
            var events = response.readEntity(new GenericType<List<ConnectionEvent>>(){});
            createEventCsv(events, fileName);
        } else {
            LOG.warn("Unable to retrieve elucidation events. Status: {} Body: {}", response.getStatus(), response.readEntity(String.class));
        }
    }

    private void writeOutTrackedIdentifiers(LocalDateTime timeToPullFrom) {
        var dateStr = DATE_TIME_FORMATTER.format(timeToPullFrom);
        var fileName = format("elucidation-tracked-identifiers-%s.csv", dateStr);

        LOG.info("Writing out trackedIdentifiers to ./export_data/{}", fileName);

        var response = httpClient.target("http://elucidation:8080/elucidate/trackedIdentifiers")
                .request()
                .get();

        if (response.getStatus() == 200) {
            var trackedConnectionIdentifiers = response.readEntity(new GenericType<List<TrackedConnectionIdentifier>>(){});
            createTrackedCsv(trackedConnectionIdentifiers, fileName);
        } else {
            LOG.warn("Unable to retrieve elucidation events. Status: {} Body: {}", response.getStatus(), response.readEntity(String.class));
        }
    }

    private void createEventCsv(List<ConnectionEvent> events, String fileName) {
        try (var out = new FileWriter("/service/data/" + fileName);
             var printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(EVENT_CSV_HEADERS))) {
            events.forEach(event -> printEvent(printer, event));
        } catch (IOException e) {
            LOG.warn("Unable to write events due to exception", e);
        }
    }

    private void printEvent(CSVPrinter printer, ConnectionEvent event) {
        try {
            printer.printRecord(
                    event.getId(),
                    event.getServiceName(),
                    event.getEventDirection(),
                    event.getCommunicationType(),
                    event.getConnectionIdentifier(),
                    event.getObservedAt());
        } catch (IOException e) {
            LOG.warn("Unable to write record due to exception", e);
        }

    }

    private void createTrackedCsv(List<TrackedConnectionIdentifier> identifiers, String fileName) {
        try (var out = new FileWriter("/service/data/" + fileName);
             var printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(TRACK_CSV_HEADERS))) {
            identifiers.forEach(identifier -> printIdentifier(printer, identifier));
        } catch (IOException e) {
            LOG.warn("Unable to write identifiers due to exception", e);
        }
    }

    private void printIdentifier(CSVPrinter printer, TrackedConnectionIdentifier identifier) {
        try {
            printer.printRecord(
                    identifier.getId(),
                    identifier.getServiceName(),
                    identifier.getCommunicationType(),
                    identifier.getConnectionIdentifier());
        } catch (IOException e) {
            LOG.warn("Unable to write record due to exception", e);
        }

    }

}
