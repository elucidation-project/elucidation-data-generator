package org.kiwiproject.elucidation.data.thermostat.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.kiwiproject.elucidation.data.thermostat.db.mapper.ThermostatMapper;
import org.kiwiproject.elucidation.data.thermostat.model.Thermostat;
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
@DisplayName("ThermostatDao")
class ThermostatDaoTest {

    private static final String THERMOSTAT_NAME = "My First Thermostat";
    private static final String LOCATION = "Hallway";
    private static final String BRAND = "Nest";

    private static SQLiteDataSource dataSource;
    private static Liquibase liquibase;

    private Jdbi jdbi;
    private ThermostatDao dao;

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

        dao = jdbi.onDemand(ThermostatDao.class);
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
    class FindAllThermostats {

        @Test
        void shouldReturnListOfThermostats_WhenSomeFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into thermostats (name, brand, location, current_temp) values (?, ?, ?, ?)",
                    THERMOSTAT_NAME,
                    BRAND,
                    LOCATION,
                    72
            ));

            var thermostats = dao.findAll();

            assertThat(thermostats)
                    .hasSize(1)
                    .extracting("name", "brand", "location", "currentTemp")
                    .contains(tuple(THERMOSTAT_NAME, BRAND, LOCATION, 72.0));
        }

        @Test
        void shouldReturnEmptyList_WhenNoThermostats() {
            assertThat(dao.findAll()).isEmpty();
        }
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnOptionalContainingFoundThermostat() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into thermostats (id, name, brand, location, current_temp) values (?, ?, ?, ?, ?)",
                    1,
                    THERMOSTAT_NAME,
                    BRAND,
                    LOCATION,
                    72
            ));

            var optionalThermostat = dao.findById(1L);

            assertThat(optionalThermostat).isPresent();

            var thermostat = optionalThermostat.orElseThrow();
            assertThat(thermostat.getId()).isEqualTo(1L);
            assertThat(thermostat.getName()).isEqualTo(THERMOSTAT_NAME);
        }

        @Test
        void shouldReturnEmptyOptionalWhenThermostatNotFound() {
            assertThat(dao.findById(1L)).isEmpty();
        }
    }

    @Nested
    class Create {

        @Test
        void shouldCreateNewThermostat() {
            var thermostat = Thermostat.builder()
                    .name(THERMOSTAT_NAME)
                    .brand(BRAND)
                    .location(LOCATION)
                    .currentTemp(73.1)
                    .build();

            var id = dao.create(thermostat);

            var thermostats = jdbi.withHandle(handle -> handle.createQuery("select * from thermostats")
                    .registerRowMapper(new ThermostatMapper())
                    .mapTo(Thermostat.class)
                    .list());

            assertThat(thermostats)
                    .hasSize(1)
                    .extracting("id", "name")
                    .contains(tuple(id, THERMOSTAT_NAME));
        }
    }

    @Nested
    class SetCurrentTemp {

        @Test
        void shouldUpdateCurrentTemp() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into thermostats (id, name, brand, location, current_temp) values (?, ?, ?, ?, ?)",
                    1,
                    THERMOSTAT_NAME,
                    BRAND,
                    LOCATION,
                    72
            ));

            int updatedCount = dao.setCurrentTemp(69, 1L);

            var thermostats = jdbi.withHandle(handle -> handle.createQuery("select * from thermostats")
                    .registerRowMapper(new ThermostatMapper())
                    .mapTo(Thermostat.class)
                    .list());

            assertThat(updatedCount).isEqualTo(1);
            assertThat(thermostats.get(0).getCurrentTemp()).isEqualTo(69);
        }

        @Test
        void shouldReturnUpdatedCountOfZero_WhenThermostatNotFound() {
            int updatedCount = dao.setCurrentTemp(10, 1L);
            assertThat(updatedCount).isZero();
        }
    }

    @Nested
    class DeleteThermostat {
        @Test
        void shouldDeleteThermostatAndReturnUpdatedCount_WhenThermostatFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into thermostats (id, name, brand, location, current_temp) values (?, ?, ?, ?, ?)",
                    1,
                    THERMOSTAT_NAME,
                    BRAND,
                    LOCATION,
                    72
            ));

            int deletedCount = dao.deleteThermostat(1L);

            var thermostats = jdbi.withHandle(handle -> handle.createQuery("select * from thermostats")
                    .registerRowMapper(new ThermostatMapper())
                    .mapTo(Thermostat.class)
                    .list());

            assertThat(deletedCount).isEqualTo(1);
            assertThat(thermostats).hasSize(0);
        }

        @Test
        void shouldReturnUpdatedCountOfZero_WhenThermostatNotFound() {
            int updatedCount = dao.deleteThermostat(1L);
            assertThat(updatedCount).isZero();
        }
    }
}
