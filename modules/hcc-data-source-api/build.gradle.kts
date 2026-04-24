plugins {
    id("hcc.java-common-conventions")
}

dependencies {
    implementation(libs.jackson.databind)

    testImplementation(libs.jackson.dataformat.yaml)
    testImplementation(libs.log4j.core)
}
