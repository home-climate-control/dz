plugins {
    id("buildlogic.java-common-conventions")
}

dependencies {

    implementation(libs.jackson.databind)
    implementation(libs.log4j.core)

    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-data-source-api"))

    testImplementation(libs.mockito)
}
