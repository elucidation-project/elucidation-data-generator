package org.kiwiproject.elucidation.data.light.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.kiwiproject.elucidation.data.light.db.mapper.SmartLightMapper;
import org.kiwiproject.elucidation.data.light.model.SmartLight;
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
@DisplayName("SmartLightDao")
class SmartLightDaoTest {

    private static final String SMART_LIGHT_NAME = "My First Smart Light";
    private static final String LOCATION = "Kitchen";
    private static final String BRAND = "Phillips-Hue";

    private static SQLiteDataSource dataSource;
    private static Liquibase liquibase;

    private Jdbi jdbi;
    private SmartLightDao dao;

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

        dao = jdbi.onDemand(SmartLightDao.class);
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
    class FindAllSmartLights {

        @Test
        void shouldReturnListOfSmartLights_WhenSomeFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into lights (name, brand, location, state, color, brightness) values (?, ?, ?, ?, ?, ?)",
                    SMART_LIGHT_NAME,
                    BRAND,
                    LOCATION,
                    SmartLight.State.ON,
                    SmartLight.Color.RED,
                    50
            ));

            var lights = dao.findAll();

            assertThat(lights)
                    .hasSize(1)
                    .extracting("name", "brand", "location", "state", "color", "brightness")
                    .contains(tuple(SMART_LIGHT_NAME, BRAND, LOCATION, SmartLight.State.ON, SmartLight.Color.RED, 50));
        }

        @Test
        void shouldReturnEmptyList_WhenNoSmartLights() {
            assertThat(dao.findAll()).isEmpty();
        }
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnOptionalContainingFoundSmartLight() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into lights (id, name, brand, location, state, color, brightness) values (?, ?, ?, ?, ?, ?, ?)",
                    1,
                    SMART_LIGHT_NAME,
                    BRAND,
                    LOCATION,
                    SmartLight.State.OFF,
                    SmartLight.Color.BLUE,
                    30
            ));

            var optionalLight = dao.findById(1L);

            assertThat(optionalLight).isPresent();

            var light = optionalLight.orElseThrow();
            assertThat(light.getId()).isEqualTo(1L);
            assertThat(light.getName()).isEqualTo(SMART_LIGHT_NAME);
        }

        @Test
        void shouldReturnEmptyOptionalWhenSmartLightNotFound() {
            assertThat(dao.findById(1L)).isEmpty();
        }
    }

    @Nested
    class Create {

        @Test
        void shouldCreateNewSmartLight() {
            var light = SmartLight.builder()
                    .name(SMART_LIGHT_NAME)
                    .brand(BRAND)
                    .location(LOCATION)
                    .state(SmartLight.State.ON)
                    .color(SmartLight.Color.PURPLE)
                    .brightness(73)
                    .build();

            var id = dao.create(light);

            var lights = jdbi.withHandle(handle -> handle.createQuery("select * from lights")
                    .registerRowMapper(new SmartLightMapper())
                    .mapTo(SmartLight.class)
                    .list());

            assertThat(lights)
                    .hasSize(1)
                    .extracting("id", "name")
                    .contains(tuple(id, SMART_LIGHT_NAME));
        }

        @Test
        void shouldCreateNewSmartLight_WithDefaults() {
            var light = SmartLight.builder()
                    .name(SMART_LIGHT_NAME)
                    .brand(BRAND)
                    .location(LOCATION)
                    .build();

            var id = dao.create(light);

            var lights = jdbi.withHandle(handle -> handle.createQuery("select * from lights")
                    .registerRowMapper(new SmartLightMapper())
                    .mapTo(SmartLight.class)
                    .list());

            assertThat(lights)
                    .hasSize(1)
                    .extracting("id", "name", "state", "color", "brightness")
                    .contains(tuple(id, SMART_LIGHT_NAME, SmartLight.State.OFF, SmartLight.Color.SOFT_WHITE, 100));
        }
    }

    @Nested
    class SaveState {
        @Test
        void shouldUpdateStateAndReturnUpdatedCount_WhenLightFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into lights (id, name, brand, location, state, color, brightness) values (?, ?, ?, ?, ?, ?, ?)",
                    1,
                    SMART_LIGHT_NAME,
                    BRAND,
                    LOCATION,
                    SmartLight.State.OFF,
                    SmartLight.Color.BLUE,
                    100
            ));

            int updatedCount = dao.saveState(SmartLight.State.ON, 1L);

            var lights = jdbi.withHandle(handle -> handle.createQuery("select * from lights")
                    .registerRowMapper(new SmartLightMapper())
                    .mapTo(SmartLight.class)
                    .list());

            assertThat(updatedCount).isEqualTo(1);
            assertThat(lights.get(0).getState()).isEqualTo(SmartLight.State.ON);
        }

        @Test
        void shouldReturnUpdatedCountOfZero_WhenLightNotFound() {
            int updatedCount = dao.saveState(SmartLight.State.ON, 1L);
            assertThat(updatedCount).isZero();
        }
    }

    @Nested
    class SetColor {
        @Test
        void shouldUpdateColorAndReturnUpdatedCount_WhenLightFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into lights (id, name, brand, location, state, color, brightness) values (?, ?, ?, ?, ?, ?, ?)",
                    1,
                    SMART_LIGHT_NAME,
                    BRAND,
                    LOCATION,
                    SmartLight.State.OFF,
                    SmartLight.Color.BLUE,
                    100
            ));

            int updatedCount = dao.setColor(SmartLight.Color.GREEN, 1L);

            var lights = jdbi.withHandle(handle -> handle.createQuery("select * from lights")
                    .registerRowMapper(new SmartLightMapper())
                    .mapTo(SmartLight.class)
                    .list());

            assertThat(updatedCount).isEqualTo(1);
            assertThat(lights.get(0).getColor()).isEqualTo(SmartLight.Color.GREEN);
        }

        @Test
        void shouldReturnUpdatedCountOfZero_WhenLightNotFound() {
            int updatedCount = dao.setColor(SmartLight.Color.ORANGE, 1L);
            assertThat(updatedCount).isZero();
        }
    }

    @Nested
    class SetBrightness {
        @Test
        void shouldUpdateBrightnessAndReturnUpdatedCount_WhenLightFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into lights (id, name, brand, location, state, color, brightness) values (?, ?, ?, ?, ?, ?, ?)",
                    1,
                    SMART_LIGHT_NAME,
                    BRAND,
                    LOCATION,
                    SmartLight.State.OFF,
                    SmartLight.Color.BLUE,
                    100
            ));

            int updatedCount = dao.setBrightness(20, 1L);

            var lights = jdbi.withHandle(handle -> handle.createQuery("select * from lights")
                    .registerRowMapper(new SmartLightMapper())
                    .mapTo(SmartLight.class)
                    .list());

            assertThat(updatedCount).isEqualTo(1);
            assertThat(lights.get(0).getBrightness()).isEqualTo(20);
        }

        @Test
        void shouldReturnUpdatedCountOfZero_WhenLightNotFound() {
            int updatedCount = dao.setBrightness(10, 1L);
            assertThat(updatedCount).isZero();
        }
    }

    @Nested
    class DeleteLight {
        @Test
        void shouldDeleteLightAndReturnUpdatedCount_WhenLightFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into lights (id, name, brand, location, state, color, brightness) values (?, ?, ?, ?, ?, ?, ?)",
                    1,
                    SMART_LIGHT_NAME,
                    BRAND,
                    LOCATION,
                    SmartLight.State.OFF,
                    SmartLight.Color.BLUE,
                    100
            ));

            int deletedCount = dao.deleteLight(1L);

            var lights = jdbi.withHandle(handle -> handle.createQuery("select * from lights")
                    .registerRowMapper(new SmartLightMapper())
                    .mapTo(SmartLight.class)
                    .list());

            assertThat(deletedCount).isEqualTo(1);
            assertThat(lights).hasSize(0);
        }

        @Test
        void shouldReturnUpdatedCountOfZero_WhenLightNotFound() {
            int updatedCount = dao.deleteLight(1L);
            assertThat(updatedCount).isZero();
        }
    }

}
