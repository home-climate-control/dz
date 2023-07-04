dependencies {

    // https://cloud.google.com/docs/authentication/production#auth-cloud-implicit-java
    implementation("com.google.cloud:google-cloud-storage:1.48.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    implementation(project(":dz3r-common"))
    implementation(project(":dz3r-http"))
}
