import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

application {
    mainClassName = "com.fortitudetec.elucidation.data.thermostat.App"
}

val dropwizardVersion: String by extra

dependencies {
    "implementation"("io.dropwizard:dropwizard-db:${dropwizardVersion}")
    "implementation"("io.dropwizard:dropwizard-jdbi3:${dropwizardVersion}")
    "implementation"("io.dropwizard:dropwizard-migrations:${dropwizardVersion}")
    "implementation"("org.xerial:sqlite-jdbc:3.30.1")
}

tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "com.fortitudetec.elucidation.data.thermostat.App"))
        }
    }

    test {
        useJUnitPlatform()
    }
}
