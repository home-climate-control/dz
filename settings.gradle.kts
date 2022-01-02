pluginManagement {
    plugins {
        id("com.github.spotbugs") version "5.0.2"
        id("net.ltgt.errorprone") version "2.0.2"
        id("org.sonarqube") version "3.3"
        id("com.autonomousapps.dependency-analysis") version "0.79.0"
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