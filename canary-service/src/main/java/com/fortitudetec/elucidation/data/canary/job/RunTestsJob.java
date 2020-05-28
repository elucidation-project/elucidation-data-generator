package com.fortitudetec.elucidation.data.canary.job;

import com.fortitudetec.elucidation.client.ElucidationEventRecorder;
import com.fortitudetec.elucidation.common.model.ConnectionEvent;
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

    private static final String[] CSV_HEADERS = { "id", "serviceName", "eventDirection", "communicationType", "observedAt" };

    private final Client httpClient;
    private final ElucidationEventRecorder eventRecorder;

    public RunTestsJob(Client httpClient, ElucidationEventRecorder eventRecorder) {
        this.httpClient = httpClient;
        this.eventRecorder = eventRecorder;
    }

    public void run() {
        LOG.info("Running data generation tests!");
        var now = LocalDateTime.now();

        try {
            new CrudDeviceCanary(httpClient, eventRecorder).runCanaryTest();

            // NOTE: This test uses devices that were set up in the test above, so if
            // that changes, then this test might need an adjustment.
            new GoodMorningWorkflowCanary(httpClient, eventRecorder).runCanaryTest();
        } catch (Exception e) {
            LOG.error("Test job threw an error", e);
        }

        try {
            // Need to let all the async stuff go through
            Thread.sleep(5000);

            writeOutElucidationEvents(now);

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
        var dateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss").format(timeToPullFrom);

        LOG.info("Writing out events to ./export_data/elucidation-events-{}.csv", dateStr);

        var response = httpClient.target("http://elucidation:8080/elucidate/events")
                .queryParam("since", timeToPullFrom.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .request()
                .get();

        if (response.getStatus() == 200) {
            var events = response.readEntity(new GenericType<List<ConnectionEvent>>(){});
            createCsv(events, dateStr);
        } else {
            LOG.warn("Unable to retrieve elucidation events. Status: {} Body: {}", response.getStatus(), response.readEntity(String.class));
        }
    }

    private void createCsv(List<ConnectionEvent> events, String dateStr) {
        try (var out = new FileWriter("/service/data/elucidation-events-" + dateStr + ".csv");
             var printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(CSV_HEADERS))) {
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

}
