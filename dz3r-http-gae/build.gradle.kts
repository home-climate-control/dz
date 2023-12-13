dependencies {

    // https://cloud.google.com/docs/authentication/production#auth-cloud-implicit-java
    implementation(libs.google.cloud.storage)
    implementation(libs.jackson.databind)

    implementation(project(":dz3r-common"))
    implementation(project(":dz3r-http"))
}
