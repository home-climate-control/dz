plugins {
    `java-library`
}

dependencies {

    implementation(libs.jackson.databind)
    implementation(libs.httpclient)

    implementation(project(":modules:hcc-common"))
    api(project(":modules:hcc-http"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.api)
}
