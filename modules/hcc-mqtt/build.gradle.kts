dependencies {


    implementation(libs.hivemq.mqtt.client.reactor)
    implementation(libs.jackson.databind)

    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-model"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.api)
    testImplementation(libs.reactor.tools)
}
