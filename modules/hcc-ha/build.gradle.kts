plugins {
    id("hcc.java-library-conventions")
}

dependencies {

    api(project(":modules:hcc-director"))
    api(project(":modules:hcc-mqtt"))

    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
}
