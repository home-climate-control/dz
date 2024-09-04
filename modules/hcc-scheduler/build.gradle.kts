plugins {
    `java-library`
}

dependencies {

    api(project(":modules:hcc-data-source-api"))
    api(project(":modules:hcc-model"))
    implementation(libs.log4j.api)
    implementation(libs.reactor.core)
    implementation(libs.jackson.databind)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.api)
}
