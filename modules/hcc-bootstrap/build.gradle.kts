plugins {
    `java-library`
}

dependencies {

    implementation(project(":modules:hcc-data-source-api"))
    api(project(":modules:hcc-common"))
    api(project(":modules:hcc-config"))
    implementation(project(":modules:hcc-director"))

    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.datatype.jdk8)
    implementation(libs.jackson.datatype.jsr310)

    // VT: NOTE: "implementation()" dependencies will migrate to
    // "runtimeOnly" after the annotation based instantiation is introduced

    // Enable the Swing console
    implementation(project(":modules:hcc-swing"))

    // Enable InfluxDB logger
    implementation(project(":modules:hcc-influxdb"))

    // Enable 1-Wire bus
    runtimeOnly(project(":modules:hcc-owapi"))

    // Enable XBee
    runtimeOnly(project(":modules:hcc-xbee"))

    // Enable MQTT
    implementation(project(":modules:hcc-mqtt"))

    // Enable Home Assistant integration
    implementation(project(":modules:hcc-ha"))

    // Enable remote control over HTTP
    implementation(project(":modules:hcc-http"))
    implementation(project(":modules:hcc-http-gae"))

    // Enable remote control via WebUI
    implementation(project(":modules:hcc-webui"))

    // Enable Google Calendar integration
    implementation(project(":modules:hcc-scheduler-gcal-v3"))

    // Enable Raspberry Pi specific hardware integration
    implementation(project(":modules:hcc-raspberry-pi"))

    // Mapstruct
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    testImplementation(libs.assertj.core)
}
