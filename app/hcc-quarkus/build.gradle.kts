plugins {
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    alias(libs.plugins.git.properties)

    java
    alias(libs.plugins.quarkus.plugin)
}

dependencies {

    implementation(project(":modules:hcc-bootstrap"))
    implementation(project(":modules:hcc-director"))
    implementation(libs.jackson.databind)

    // Enable MQTT
    implementation(project(":modules:hcc-mqtt"))

    // Enable the Swing console
    runtimeOnly(project(":modules:hcc-swing"))

    // Enable InfluxDB logger
    runtimeOnly(project(":modules:hcc-influxdb"))

    // Enable 1-Wire bus
    runtimeOnly(project(":modules:hcc-owapi"))

    // Enable XBee
    runtimeOnly(project(":modules:hcc-xbee"))

    // Enable remote control over HTTP
    runtimeOnly(project(":modules:hcc-http"))
    runtimeOnly(project(":modules:hcc-http-gae"))

    // Enable remote control via WebUI
    runtimeOnly(project(":modules:hcc-webui"))

    // Enable Google Calendar integration
    runtimeOnly(project(":modules:hcc-scheduler-gcal-v3"))

    // Enable Raspberry Pi specific hardware integration
    runtimeOnly(project(":modules:hcc-raspberry-pi"))

    testImplementation(libs.rest.assured)
    testImplementation(libs.assertj.core)
}

// Quarkus additions
dependencies {

    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.config.yaml)
    implementation(libs.quarkus.resteasy.reactive)
    testImplementation(libs.quarkus.junit5)
}

// Mapstruct
dependencies {

    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
}
