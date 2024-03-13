plugins {
    `java-library`
}

dependencies {

    implementation(libs.jackson.databind)
    implementation(libs.httpclient)

    implementation(project(":modules:hcc-common"))
    api(project(":modules:hcc-http"))
}
