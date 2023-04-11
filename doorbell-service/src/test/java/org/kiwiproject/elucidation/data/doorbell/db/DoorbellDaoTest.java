package org.kiwiproject.elucidation.data.doorbell.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.kiwiproject.elucidation.data.doorbell.db.mapper.DoorbellMapper;
import org.kiwiproject.elucidation.data.doorbell.model.Doorbell;
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
@DisplayName("DoorbellDao")
class DoorbellDaoTest {

    private static final String DOORBELL_NAME = "My First Doorbell";
    private static final String BRAND = "Nest";

    private static SQLiteDataSource dataSource;
    private static Liquibase liquibase;

    private Jdbi jdbi;
    private DoorbellDao dao;

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

        dao = jdbi.onDemand(DoorbellDao.class);
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
    class FindAllDoorbells {

        @Test
        void shouldReturnListOfDoorbells_WhenSomeFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into doorbells (name, brand) values (?, ?)",
                    DOORBELL_NAME,
                    BRAND
            ));

            var doorbells = dao.findAll();

            assertThat(doorbells)
                    .hasSize(1)
                    .extracting("name", "brand")
                    .contains(tuple(DOORBELL_NAME, BRAND));
        }

        @Test
        void shouldReturnEmptyList_WhenNoDoorbells() {
            assertThat(dao.findAll()).isEmpty();
        }
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnOptionalContainingFoundDoorbell() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into doorbells (id, name, brand) values (?, ?, ?)",
                    1,
                    DOORBELL_NAME,
                    BRAND
            ));

            var optionalDoorbell = dao.findById(1L);

            assertThat(optionalDoorbell).isPresent();

            var doorbell = optionalDoorbell.orElseThrow();
            assertThat(doorbell.getId()).isEqualTo(1L);
            assertThat(doorbell.getName()).isEqualTo(DOORBELL_NAME);
        }

        @Test
        void shouldReturnEmptyOptionalWhenDoorbellNotFound() {
            assertThat(dao.findById(1L)).isEmpty();
        }
    }

    @Nested
    class Create {

        @Test
        void shouldCreateNewDoorbell() {
            var doorbell = Doorbell.builder()
                    .name(DOORBELL_NAME)
                    .brand(BRAND)
                    .build();

            var id = dao.create(doorbell);

            var doorbells = jdbi.withHandle(handle -> handle.createQuery("select * from doorbells")
                    .registerRowMapper(new DoorbellMapper())
                    .mapTo(Doorbell.class)
                    .list());

            assertThat(doorbells)
                    .hasSize(1)
                    .extracting("id", "name")
                    .contains(tuple(id, DOORBELL_NAME));
        }
    }

    @Nested
    class DeleteDoorbell {
        @Test
        void shouldDeleteDoorbellAndReturnUpdatedCount_WhenDoorbellFound() {
            jdbi.withHandle(handle -> handle.execute(
                    "insert into doorbells (id, name, brand) values (?, ?, ?)",
                    1,
                    DOORBELL_NAME,
                    BRAND
            ));

            int deletedCount = dao.deleteDoorbell(1L);

            var doorbells = jdbi.withHandle(handle -> handle.createQuery("select * from doorbells")
                    .registerRowMapper(new DoorbellMapper())
                    .mapTo(Doorbell.class)
                    .list());

            assertThat(deletedCount).isEqualTo(1);
            assertThat(doorbells).hasSize(0);
        }

        @Test
        void shouldReturnUpdatedCountOfZero_WhenDoorbellNotFound() {
            int updatedCount = dao.deleteDoorbell(1L);
            assertThat(updatedCount).isZero();
        }
    }
}
