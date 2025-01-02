plugins {
    id("buildlogic.java-common-conventions")
}

dependencies {

    implementation(libs.jackson.databind)

    implementation(project(":modules:hcc-data-source-api"))
    implementation(project(":modules:hcc-model"))

    testImplementation(libs.jackson.datatype.jdk8)
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.jackson.dataformat.yaml)
    testImplementation(libs.log4j.core)
}
