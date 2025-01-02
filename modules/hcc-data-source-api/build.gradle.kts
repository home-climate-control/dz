plugins {
    id("buildlogic.java-common-conventions")
}

dependencies {
    implementation(libs.jackson.databind)

    implementation(project(":modules:hcc-common"))

    testImplementation(libs.jackson.dataformat.yaml)
    testImplementation(libs.log4j.core)
}
