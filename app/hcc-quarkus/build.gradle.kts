plugins {
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    alias(libs.plugins.git.properties)

    java
    alias(libs.plugins.quarkus.plugin)
    id("hcc.java-common-conventions")
    id("hcc.quarkus-conventions")
}

dependencies {

    implementation(libs.jackson.databind)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    implementation(project(":modules:hcc-bootstrap"))
    implementation(project(":modules:hcc-director"))

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
}
