dependencies {

    implementation(libs.google.api.services.calendar)
    implementation(libs.google.oauth.client.jetty)

    implementation(project(":dz3r-common"))
    implementation(project(":dz3r-scheduler"))
}
