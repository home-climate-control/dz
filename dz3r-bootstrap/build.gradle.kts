plugins {
    java
}

dependencies {

    implementation(project(":hcc-data-source-api"))
    api(project(":dz3r-common"))
    api(project(":dz3r-config"))
    implementation(project(":dz3r-director"))

    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.datatype.jdk8)
    implementation(libs.jackson.datatype.jsr310)

    // VT: NOTE: "implementation()" dependencies will migrate to
    // "runtimeOnly" after the annotation based instantiation is introduced

    // Enable the Swing console
    implementation(project(":dz3r-swing"))

    // Enable InfluxDB logger
    implementation(project(":dz3r-influxdb"))

    // Enable 1-Wire bus
    runtimeOnly(project(":dz3r-owapi"))

    // Enable XBee
    runtimeOnly(project(":dz3r-xbee"))

    // Enable MQTT
    implementation(project(":dz3r-mqtt"))

    // Enable Home Assistant integration
    implementation(project(":dz3r-ha"))

    // Enable remote control over HTTP
    implementation(project(":dz3r-http"))
    implementation(project(":dz3r-http-gae"))

    // Enable remote control via WebUI
    implementation(project(":dz3r-webui"))

    // Enable Google Calendar integration
    implementation(project(":dz3r-scheduler-gcal-v3"))

    // Enable Raspberry Pi specific hardware integration
    implementation(project(":dz3r-raspberry-pi"))

    // Mapstruct
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    testImplementation(libs.assertj.core)
}
