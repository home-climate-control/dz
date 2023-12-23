plugins {
    java
}

dependencies {

    api(project(":dz3r-director"))
    api(project(":dz3r-mqtt"))

    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jdk8)
    implementation(libs.jackson.datatype.jsr310)

    implementation(libs.hivemq.mqtt.client.reactor)
}
