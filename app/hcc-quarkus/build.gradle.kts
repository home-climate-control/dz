plugins {
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    alias(libs.plugins.git.properties)

    java
    alias(libs.plugins.quarkus.plugin)
}

val quarkusPlatformGroupId = "io.quarkus.platform"
val quarkusPlatformArtifactId = "quarkus-bom"

dependencies {

    implementation(project(":modules:hcc-bootstrap"))
    implementation(project(":modules:hcc-director"))
    implementation(libs.jackson.databind)

    // Enable the Swing console
    runtimeOnly(project(":modules:hcc-swing"))

    // Enable InfluxDB logger
    runtimeOnly(project(":modules:hcc-influxdb"))

    // Enable 1-Wire bus
    runtimeOnly(project(":modules:hcc-owapi"))

    // Enable XBee
    runtimeOnly(project(":modules:hcc-xbee"))

    // Enable MQTT
    implementation(project(":modules:hcc-mqtt"))

    // Enable remote control over HTTP
    runtimeOnly(project(":modules:hcc-http"))
    runtimeOnly(project(":modules:hcc-http-gae"))

    // Enable remote control via WebUI
    runtimeOnly(project(":modules:hcc-webui"))

    // Enable Google Calendar integration
    runtimeOnly(project(":modules:hcc-scheduler-gcal-v3"))

    // Enable Raspberry Pi specific hardware integration
    runtimeOnly(project(":modules:hcc-raspberry-pi"))

    // Quarkus additions
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.config.yaml)
    implementation(libs.quarkus.resteasy.reactive)
    testImplementation(libs.quarkus.junit5)
    testImplementation(libs.rest.assured)

    // Mapstruct
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    testImplementation(libs.assertj.core)
}
