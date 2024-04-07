plugins {
    application
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    alias(libs.plugins.git.properties)
}

application {
    applicationName = "hcc"
    mainClass.set("net.sf.dz3r.runtime.standalone.HccApplication")
}

dependencies {

    implementation(libs.jackson.databind)

    implementation(project(":modules:hcc-bootstrap"))
    implementation(project(":modules:hcc-common"))

    // Enable the Swing console
    runtimeOnly(project(":modules:hcc-swing"))

    // Enable InfluxDB logger
    runtimeOnly(project(":modules:hcc-influxdb"))

    // Enable 1-Wire bus
    runtimeOnly(project(":modules:hcc-owapi"))

    // Enable XBee
    runtimeOnly(project(":modules:hcc-xbee"))

    // Enable MQTT
    runtimeOnly(project(":modules:hcc-mqtt"))

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
