dependencies {

    implementation(project(":submodules:xbee-api-reactive:xbee-api"))
    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-driver"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.api)
    testImplementation(libs.reactor.tools)
}
