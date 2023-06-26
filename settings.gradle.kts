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

// One ring to find^Wrun them all
include("dz3r-spring")

// One ring to find^Wrun them all... with SpringBoot :O
include("dz3r-app-springboot")
// ...and with Quarkus
include("dz3r-app-quarkus")

rootProject.name = "dz3-master"
