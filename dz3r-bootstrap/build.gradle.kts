plugins {
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    id("com.gorylenko.gradle-git-properties")

    java
}

dependencies {

    api(project(":dz3r-common"))
    api(project(":dz3r-config"))
    implementation(project(":dz3r-director"))

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    // VTL NOTE: "implementation()" dependencies will migrate to
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
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    testImplementation("org.assertj:assertj-core:3.21.0")
}
