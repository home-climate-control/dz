dependencies {
    implementation(libs.jackson.databind)

    implementation(project(":modules:hcc-common"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.jackson.dataformat.yaml)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testImplementation(libs.log4j.core)
}
