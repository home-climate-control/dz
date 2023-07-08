plugins {
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    id("com.gorylenko.gradle-git-properties")

    java
    id("io.quarkus")
}

val quarkusPlatformGroupId = "io.quarkus.platform"
val quarkusPlatformArtifactId = "quarkus-bom"
val quarkusPlatformVersion = "3.0.1.Final"

dependencies {

    implementation(project(":dz3r-bootstrap"))
    implementation(project(":dz3r-director"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

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
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-resteasy-reactive")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")

    // Mapstruct
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    testImplementation("org.assertj:assertj-core:3.21.0")
}
