dependencies {

    implementation(libs.google.api.services.calendar)
    implementation(libs.google.oauth.client.jetty)

    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-scheduler"))
}
