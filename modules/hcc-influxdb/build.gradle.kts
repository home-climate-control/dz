
dependencies {

    implementation(libs.influxdb)

    implementation(project(":modules:hcc-director"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.api)
    testImplementation(libs.reactor.tools)
}
