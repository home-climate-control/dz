plugins {
    id("hcc.java-common-conventions")
}

dependencies {

    implementation(libs.jackson.databind)

    implementation(project(":modules:hcc-data-source-api"))
    implementation(project(":modules:hcc-model"))

    testImplementation(libs.jackson.dataformat.yaml)
    testImplementation(libs.log4j.core)
}
