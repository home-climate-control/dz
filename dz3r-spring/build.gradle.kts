plugins {
    application
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    id("com.gorylenko.gradle-git-properties")
}

application {
    applicationName = "dz"
    mainClass.set("net.sf.dz3.runtime.Container")
}

dependencies {

    implementation("org.springframework:spring-context:5.3.27")

    implementation(project(":dz3r-common"))
    implementation(project(":dz3r-director"))

    // Enable the Swing console
    runtimeOnly(project(":dz3r-swing"))

    // Enable InfluxDB logger
    runtimeOnly(project(":dz3r-influxdb"))

    // Enable 1-Wire bus
    runtimeOnly(project(":dz3r-owapi"))

    // Enable XBee
    runtimeOnly(project(":dz3r-xbee"))

    // Enable MQTT
    runtimeOnly(project(":dz3r-mqtt"))

    // Enable remote control over HTTP
    runtimeOnly(project(":dz3r-http"))
    runtimeOnly(project(":dz3r-http-gae"))

    // Enable remote control via WebUI
    runtimeOnly(project(":dz3r-webui"))

    // Enable Google Calendar integration
    runtimeOnly(project(":dz3r-scheduler-gcal-v3"))

    // Enable Raspberry Pi specific hardware integration
    runtimeOnly(project(":dz3r-raspberry-pi"))

    // Enable Micrometer integration
    implementation("io.micrometer:micrometer-registry-influx:1.11.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.11.0")
    implementation("org.springframework:spring-webflux:5.3.27")
    implementation("io.projectreactor.netty:reactor-netty:1.1.6")
}
