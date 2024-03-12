plugins {
    `java-library`
}

dependencies {

    // https://cloud.google.com/docs/authentication/production#auth-cloud-implicit-java
    // VT: FIXME: The only thing necessary from here is Gson, it can be replaced with Jackson here
    implementation(libs.google.cloud.storage)
    implementation(libs.jackson.databind)

    implementation(project(":modules:hcc-common"))
    api(project(":modules:hcc-http"))
}
