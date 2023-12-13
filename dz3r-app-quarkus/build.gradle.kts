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

    implementation(project(":dz3r-bootstrap"))
    implementation(project(":dz3r-director"))
    implementation(libs.jackson.databind)

    // Enable the Swing console
    runtimeOnly(project(":dz3r-swing"))

    // Enable InfluxDB logger
    runtimeOnly(project(":dz3r-influxdb"))

    // Enable 1-Wire bus
    runtimeOnly(project(":dz3r-owapi"))

    // Enable XBee
    runtimeOnly(project(":dz3r-xbee"))

    // Enable MQTT
    implementation(project(":dz3r-mqtt"))

    // Enable remote control over HTTP
    runtimeOnly(project(":dz3r-http"))
    runtimeOnly(project(":dz3r-http-gae"))

    // Enable remote control via WebUI
    runtimeOnly(project(":dz3r-webui"))

    // Enable Google Calendar integration
    runtimeOnly(project(":dz3r-scheduler-gcal-v3"))

    // Enable Raspberry Pi specific hardware integration
    runtimeOnly(project(":dz3r-raspberry-pi"))

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
