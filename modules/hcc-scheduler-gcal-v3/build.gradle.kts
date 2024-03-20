dependencies {

    implementation(libs.google.api.services.calendar)
    implementation(libs.google.oauth.client.jetty)

    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-scheduler"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testImplementation(libs.reactor.tools)
}
