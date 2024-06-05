dependencies {

    implementation(libs.jackson.databind)
    implementation(libs.log4j.core)

    implementation(project(":modules:hcc-common"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testImplementation(libs.reactor.test)
    testImplementation(libs.reactor.tools)
}
