val hivemqMqttVersion: String by project
val jacksonVersion: String by project

dependencies {


    implementation("com.hivemq:hivemq-mqtt-client-reactor:$hivemqMqttVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    implementation(project(":dz3r-common"))
    implementation(project(":dz3r-model"))
}
