dependencies {

    implementation(libs.google.api.services.calendar)
    implementation(libs.google.oauth.client.jetty)
    implementation(libs.google.http.client.jackson2)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)

    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-scheduler"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testImplementation(libs.reactor.tools)
}
