plugins {
    id("buildlogic.java-library-conventions")
}

dependencies {

    api(libs.hivemq.mqtt.client)
    implementation(libs.jackson.databind)

    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-data-source-api"))
    implementation(project(":modules:hcc-model"))
}
