package com.fortitudetec.elucidation.data.home.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortitudetec.elucidation.data.home.db.mapper.WorkflowMapper;
import com.fortitudetec.elucidation.data.home.model.Device;
import com.fortitudetec.elucidation.data.home.model.Workflow;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@SuppressWarnings("java:S100")
@DisplayName("WorkflowDao")
class WorkflowDaoTest {

    private static final String WORKFLOW_NAME = "My First Workflow";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static SQLiteDataSource dataSource;
    private static Liquibase liquibase;

    private Jdbi jdbi;
    private WorkflowDao dao;
    private String workflowStepJson;

    @BeforeAll
    static void migrationSetup() throws SQLException, DatabaseException {
        dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:unit.db");

        var conn = dataSource.getConnection();
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
        liquibase = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), database);
    }

    @BeforeEach
    void setupJdbi() throws LiquibaseException, JsonProcessingException {
        setupDbObjects();

        dao = jdbi.onDemand(WorkflowDao.class);

        var device = Device.builder()
                .name("Nest Thermostat")
                .deviceType(Device.DeviceType.THERMOSTAT)
                .deviceTypeId(1L)
                .build();

        var steps = List.of(
                Map.of("description", "Step 1", "device", device, "eventAction", "SET_TEMP", "eventInfo", Map.of("temp", 72), "nextStepDelayInSeconds", 0)
        );

        workflowStepJson = MAPPER.writeValueAsString(steps);
    }

    private void setupDbObjects() throws LiquibaseException {
        liquibase.update(new Contexts());

        jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.open();
    }

    @AfterAll
    static void cleanupDatabase() throws IOException {
        Files.deleteIfExists(Path.of("./unit.db"));
    }

    @AfterEach
    void dropDbRecords() throws DatabaseException {
        liquibase.dropAll();
    }

    @Nested
    class FindAllWorkflows {

        @Test
        void shouldReturnListOfWorkflows_WhenSomeFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into workflows (name, step_json) values (?, ?)",
                    WORKFLOW_NAME,
                    workflowStepJson
            ));

            var workflows = dao.findAll();

            assertThat(workflows)
                    .hasSize(1)
                    .extracting("name", "stepJson")
                    .contains(tuple(WORKFLOW_NAME, workflowStepJson));
        }

        @Test
        void shouldReturnEmptyList_WhenNoWorkflows() {
            assertThat(dao.findAll()).isEmpty();
        }
    }

    @Nested
    class Create {

        @Test
        void shouldCreateNewWorkflow() {
            var workflow = Workflow.builder()
                    .name(WORKFLOW_NAME)
                    .stepJson(workflowStepJson)
                    .build();

            var id = dao.create(workflow);

            var workflows = jdbi.withHandle(handle -> handle.createQuery("select * from workflows")
                    .registerRowMapper(new WorkflowMapper())
                    .mapTo(Workflow.class)
                    .list());

            assertThat(workflows)
                    .hasSize(1)
                    .extracting("id", "name")
                    .contains(tuple(id, WORKFLOW_NAME));
        }
    }

    @Nested
    class DeleteWorkflow {
        @Test
        void shouldDeleteWorkflowAndReturnUpdatedCount_WhenWorkflowFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into workflows (id, name, step_json) values (?, ?, ?)",
                    1,
                    WORKFLOW_NAME,
                    workflowStepJson
            ));

            int deletedCount = dao.deleteWorkflow(1L);

            var workflows = jdbi.withHandle(handle -> handle.createQuery("select * from workflows")
                    .registerRowMapper(new WorkflowMapper())
                    .mapTo(Workflow.class)
                    .list());

            assertThat(deletedCount).isEqualTo(1);
            assertThat(workflows).hasSize(0);
        }

        @Test
        void shouldReturnUpdatedCountOfZero_WhenWorkflowNotFound() {
            int updatedCount = dao.deleteWorkflow(1L);
            assertThat(updatedCount).isZero();
        }
    }
}
