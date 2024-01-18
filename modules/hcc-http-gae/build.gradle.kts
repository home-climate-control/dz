dependencies {

    // https://cloud.google.com/docs/authentication/production#auth-cloud-implicit-java
    implementation(libs.google.cloud.storage)
    implementation(libs.jackson.databind)

    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-http"))
}
