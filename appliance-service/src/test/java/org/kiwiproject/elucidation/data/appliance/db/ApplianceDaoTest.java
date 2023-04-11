package org.kiwiproject.elucidation.data.appliance.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.kiwiproject.elucidation.data.appliance.db.mapper.ApplianceMapper;
import org.kiwiproject.elucidation.data.appliance.model.Appliance;
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

@SuppressWarnings("java:S100")
@DisplayName("ApplianceDao")
class ApplianceDaoTest {

    private static final String APPLIANCE_NAME = "My First Appliance";
    private static final String BRAND = "Nest";
    private static final String LOCATION = "Kitchen";

    private static SQLiteDataSource dataSource;
    private static Liquibase liquibase;

    private Jdbi jdbi;
    private ApplianceDao dao;

    @BeforeAll
    static void migrationSetup() throws SQLException, DatabaseException {
        dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:unit.db");

        var conn = dataSource.getConnection();
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
        liquibase = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), database);
    }

    @BeforeEach
    void setupJdbi() throws LiquibaseException {
        setupDbObjects();

        dao = jdbi.onDemand(ApplianceDao.class);
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
    class FindAllAppliances {

        @Test
        void shouldReturnListOfAppliances_WhenSomeFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into appliances (name, brand, location, state) values (?, ?, ?, ?)",
                    APPLIANCE_NAME,
                    BRAND,
                    LOCATION,
                    Appliance.State.OFF
            ));

            var appliances = dao.findAll();

            assertThat(appliances)
                    .hasSize(1)
                    .extracting("name", "brand", "location", "state")
                    .contains(tuple(APPLIANCE_NAME, BRAND, LOCATION, Appliance.State.OFF));
        }

        @Test
        void shouldReturnEmptyList_WhenNoAppliances() {
            assertThat(dao.findAll()).isEmpty();
        }
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnOptionalContainingFoundAppliance() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into appliances (id, name, brand, location, state) values (?, ?, ?, ?, ?)",
                    1,
                    APPLIANCE_NAME,
                    BRAND,
                    LOCATION,
                    Appliance.State.ON
            ));

            var optionalAppliance = dao.findById(1L);

            assertThat(optionalAppliance).isPresent();

            var appliance = optionalAppliance.orElseThrow();
            assertThat(appliance.getId()).isEqualTo(1L);
            assertThat(appliance.getName()).isEqualTo(APPLIANCE_NAME);
        }

        @Test
        void shouldReturnEmptyOptionalWhenApplianceNotFound() {
            assertThat(dao.findById(1L)).isEmpty();
        }
    }

    @Nested
    class Create {

        @Test
        void shouldCreateNewAppliance() {
            var appliance = Appliance.builder()
                    .name(APPLIANCE_NAME)
                    .brand(BRAND)
                    .location(LOCATION)
                    .state(Appliance.State.OFF)
                    .build();

            var id = dao.create(appliance);

            var appliances = jdbi.withHandle(handle -> handle.createQuery("select * from appliances")
                    .registerRowMapper(new ApplianceMapper())
                    .mapTo(Appliance.class)
                    .list());

            assertThat(appliances)
                    .hasSize(1)
                    .extracting("id", "name")
                    .contains(tuple(id, APPLIANCE_NAME));
        }
    }

    @Nested
    class DeleteAppliance {
        @Test
        void shouldDeleteApplianceAndReturnUpdatedCount_WhenApplianceFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into appliances (id, name, brand, location, state) values (?, ?, ?, ?, ?)",
                    1,
                    APPLIANCE_NAME,
                    BRAND,
                    LOCATION,
                    Appliance.State.ON
            ));

            int deletedCount = dao.deleteAppliance(1L);

            var appliances = jdbi.withHandle(handle -> handle.createQuery("select * from appliances")
                    .registerRowMapper(new ApplianceMapper())
                    .mapTo(Appliance.class)
                    .list());

            assertThat(deletedCount).isEqualTo(1);
            assertThat(appliances).hasSize(0);
        }

        @Test
        void shouldReturnUpdatedCountOfZero_WhenApplianceNotFound() {
            int updatedCount = dao.deleteAppliance(1L);
            assertThat(updatedCount).isZero();
        }
    }
}
