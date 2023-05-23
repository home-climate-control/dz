plugins {
//    application
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    id("com.gorylenko.gradle-git-properties")

    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

//application {
//    applicationName = "dz"
//    mainClass.set("net.sf.dz3.runtime.Container")
//}

dependencies {

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

    // SpringBoot additions
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    runtimeOnly("io.micrometer:micrometer-registry-influx")
    runtimeOnly("io.micrometer:micrometer-registry-jmx")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}
