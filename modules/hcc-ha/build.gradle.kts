plugins {
    `java-library`
}

dependencies {

    api(project(":modules:hcc-director"))
    api(project(":modules:hcc-mqtt"))

    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jdk8)
    implementation(libs.jackson.datatype.jsr310)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.api)
}
