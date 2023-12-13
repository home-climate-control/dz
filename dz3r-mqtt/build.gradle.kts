dependencies {


    implementation(libs.hivemq.mqtt.client.reactor)
    implementation(libs.jackson.databind)

    implementation(project(":dz3r-common"))
    implementation(project(":dz3r-model"))
}
