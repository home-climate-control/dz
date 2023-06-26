pluginManagement {
    plugins {
        id("net.ltgt.errorprone") version "3.1.0"
        id("org.sonarqube") version "4.0.0.2929"
        id("com.gorylenko.gradle-git-properties") version "2.4.1"

        id("org.springframework.boot") version "3.1.0"
        id("io.spring.dependency-management") version "1.1.0"

        id("io.quarkus") version "3.0.1.Final"
    }
}

// External dependencies

include("automation-hat-driver")
include("owapi-reborn")
include("servomaster:servomaster-common")
include("xbee-api-reactive:xbee-api")

// Modules

include("dz3r-bootstrap")
include("dz3r-common")
include("dz3r-config")
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

// Run options

// One ring to find^Wrun them all

// This is the legacy application that reads the XML configuration. Will
// cease to exist soon, that configuration is too cumbersome. Name is out of
// the pattern, kept like that for compatibility.

include("dz3r-spring")

// This is the minimal application that reads the configuration from the
// given file, and contains no extra bells and whistles. May be ideal to run
// with stable and known good configurations, but may not be the best choice
// to experiment with.

// VT: FIXME: Coming up
//include("dz3r-app-minimal")

// This is the SpringBoot application, with all SpringBoot bells and
// whistles (including emitting metrics) included. See application.xml for
// details.

// Run with:
//   ./gradlew bootRun --args='--spring.profiles.active=<your-profile>'


include("dz3r-app-springboot")

// This is the Quarkus application, with all Quarkus bells and
// whistles (including emitting metrics) included. See application.xml for
// details.

// Run with (for starters; native app instructions coming):
//   QUARKUS_PROFILE=<your-profile> ./gradlew quarkusDev

include("dz3r-app-quarkus")

rootProject.name = "dz3-master"
