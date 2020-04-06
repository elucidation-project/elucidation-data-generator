allprojects {
    apply(plugin = "java")
    group = "com.fortitudetec.elucidation.data"
    version = "1.0.0"
}

plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0" apply false
}

subprojects {
    apply(plugin = "application")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        mavenCentral()
    }

    // Runtime dependency versions
    val lombokVersion: String by extra("1.18.12")
    val dropwizardVersion: String by extra( "2.0.6")

    // Test dependency versions
    val junitVersion: String by extra("5.6.1")
    val assertjVersion: String by extra("3.15.0")
    val mockitoVersion: String by extra("3.3.3")

    dependencies {
        "implementation"("io.dropwizard:dropwizard-core:${dropwizardVersion}")

        "compileOnly"("org.projectlombok:lombok:${lombokVersion}")
        "annotationProcessor"("org.projectlombok:lombok:${lombokVersion}")

        "testImplementation"("org.junit.jupiter:junit-jupiter:${junitVersion}")
        "testImplementation"("org.assertj:assertj-core:${assertjVersion}")
        "testImplementation"("org.mockito:mockito-core:${mockitoVersion}")
        "testImplementation"("io.dropwizard:dropwizard-testing:${dropwizardVersion}")

        "testCompileOnly"("org.projectlombok:lombok:${lombokVersion}")
        "testAnnotationProcessor"("org.projectlombok:lombok:${lombokVersion}")
    }
}

tasks {
    val stage by registering {
        dependsOn("build", "clean")
    }
}

