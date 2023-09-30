plugins {
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    id("com.gorylenko.gradle-git-properties")

    java
}

val hivemqMqttVersion: String by project
val jacksonVersion: String by project

dependencies {

    api(project(":dz3r-director"))
    api(project(":dz3r-mqtt"))

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("com.hivemq:hivemq-mqtt-client-reactor:$hivemqMqttVersion")
}
