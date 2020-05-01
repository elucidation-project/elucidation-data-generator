package com.fortitudetec.elucidation.data.home.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fortitudetec.elucidation.data.home.db.mapper.DeviceMapper;
import com.fortitudetec.elucidation.data.home.model.Device;
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
@DisplayName("DeviceDao")
class DeviceDaoTest {

    private static final String DEVICE_NAME = "My First Device";

    private static SQLiteDataSource dataSource;
    private static Liquibase liquibase;

    private Jdbi jdbi;
    private DeviceDao dao;

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

        dao = jdbi.onDemand(DeviceDao.class);
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
    class FindAllDevices {

        @Test
        void shouldReturnListOfDevices_WhenSomeFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into devices (name, device_type, device_type_id) values (?, ?, ?)",
                    DEVICE_NAME,
                    Device.DeviceType.THERMOSTAT,
                    42
            ));

            var devices = dao.findAll();

            assertThat(devices)
                    .hasSize(1)
                    .extracting("name", "deviceType", "deviceTypeId")
                    .contains(tuple(DEVICE_NAME, Device.DeviceType.THERMOSTAT, 42L));
        }

        @Test
        void shouldReturnEmptyList_WhenNoDevices() {
            assertThat(dao.findAll()).isEmpty();
        }
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnOptionalContainingFoundDevices() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into devices (id, name, device_type, device_type_id) values (?, ?, ?, ?)",
                    1,
                    DEVICE_NAME,
                    Device.DeviceType.APPLIANCE,
                    42L
            ));

            var optionalDevice = dao.findById(1L);

            assertThat(optionalDevice).isPresent();

            var device = optionalDevice.orElseThrow();
            assertThat(device.getId()).isEqualTo(1L);
            assertThat(device.getName()).isEqualTo(DEVICE_NAME);
        }

        @Test
        void shouldReturnEmptyOptionalWhenDeviceNotFound() {
            assertThat(dao.findById(1L)).isEmpty();
        }
    }

    @Nested
    class Create {

        @Test
        void shouldCreateNewDevice() {
            var device = Device.builder()
                    .name(DEVICE_NAME)
                    .deviceType(Device.DeviceType.DOORBELL)
                    .deviceTypeId(21L)
                    .build();

            var id = dao.create(device);

            var devices = jdbi.withHandle(handle -> handle.createQuery("select * from devices")
                    .registerRowMapper(new DeviceMapper())
                    .mapTo(Device.class)
                    .list());

            assertThat(devices)
                    .hasSize(1)
                    .extracting("id", "name")
                    .contains(tuple(id, DEVICE_NAME));
        }
    }

    @Nested
    class DeleteDevice {
        @Test
        void shouldDeleteDeviceAndReturnUpdatedCount_WhenDeviceFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into devices (id, name, device_type, device_type_id) values (?, ?, ?, ?)",
                    1,
                    DEVICE_NAME,
                    Device.DeviceType.APPLIANCE,
                    33L
            ));

            int deletedCount = dao.deleteDevice(1L);

            var devices = jdbi.withHandle(handle -> handle.createQuery("select * from devices")
                    .registerRowMapper(new DeviceMapper())
                    .mapTo(Device.class)
                    .list());

            assertThat(deletedCount).isEqualTo(1);
            assertThat(devices).hasSize(0);
        }

        @Test
        void shouldReturnUpdatedCountOfZero_WhenDeviceNotFound() {
            int updatedCount = dao.deleteDevice(1L);
            assertThat(updatedCount).isZero();
        }
    }
}
