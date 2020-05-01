package com.fortitudetec.elucidation.data.home;

import com.fortitudetec.elucidation.data.home.config.AppConfig;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ExtendWith(DropwizardExtensionsSupport.class)
@DisplayName("App")
class AppTest {

    private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("unit-config.yml");

    private static final DropwizardAppExtension<AppConfig> APP = new DropwizardAppExtension<>(
            App.class, CONFIG_PATH,
            ConfigOverride.config("database.url", "jdbc:sqlite::memory::")
    );

    @BeforeEach
    void setUp() throws Exception {
        APP.getApplication().run("db", "migrate", CONFIG_PATH);
    }

    @AfterAll
    static void removeDb() throws IOException {
        Files.deleteIfExists(Path.of("./:memory::"));
    }

    @Test
    void testRun() {
        // TODO: Add back in once resource exists
//        assertThat(APP.getEnvironment().jersey().getResourceConfig().isRegistered(ApplianceResource.class)).isTrue();
    }

}
