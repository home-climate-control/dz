enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    plugins {
        id("com.github.spotbugs") version "5.0.2"
        id("net.ltgt.errorprone") version "2.0.2"
        id("org.sonarqube") version "3.3"
        id("com.autonomousapps.dependency-analysis") version "0.79.0"
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {

            version("junitVersion", "5.8.2")
            version("log4jVersion", "2.17.0")
            version("reactorVersion", "3.4.11")

            alias("assertj").to("org.assertj:assertj-core:3.21.0")
            alias("errorprone").to("com.google.errorprone:error_prone_core:2.10.0")
            alias("junit-jupiter-api").to("org.junit.jupiter","junit-jupiter-api").versionRef("junitVersion")
            alias("junit-jupiter-engine").to("org.junit.jupiter", "junit-jupiter-engine").versionRef("junitVersion")
            alias("junit-jupiter-params").to("org.junit.jupiter","junit-jupiter-params").versionRef("junitVersion")
            alias("log4j2-api").to("org.apache.logging.log4j","log4j-api").versionRef("log4jVersion")
            alias("log4j2-core").to("org.apache.logging.log4j", "log4j-core").versionRef("log4jVersion")
            alias("mockito").to("org.mockito:mockito-core:4.2.0")
            alias("reactor-core").to("io.projectreactor", "reactor-core").versionRef("reactorVersion")
            alias("reactor-test").to("io.projectreactor", "reactor-test").versionRef("reactorVersion")
            alias("reactor-tools").to("io.projectreactor", "reactor-tools").versionRef("reactorVersion")
        }
    }
}

// External dependencies

include("automation-hat-driver")
include("jukebox:jukebox-jmx")
include("owapi-reborn")
include("servomaster:servomaster-common")
include("xbee-api-reactive:xbee-api")

// Modules

include("dz3r-common")
include("dz3r-director")
include("dz3r-driver")
include("dz3r-http")
include("dz3r-http-gae")
include("dz3r-influxdb")
include("dz3r-model")
include("dz3r-mqtt")
include("dz3r-raspberry-pi")
include("dz3r-owapi")
include("dz3r-scheduler")
include("dz3r-scheduler-gcal-v3")
include("dz3r-swing")
include("dz3r-webui")
include("dz3r-xbee")

// One ring to find^Wrun them all
include("dz3r-spring")


rootProject.name = "dz3-master"
