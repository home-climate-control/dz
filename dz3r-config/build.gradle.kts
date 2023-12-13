dependencies {

    implementation(project(":dz3r-model"))
    implementation(libs.jackson.databind)

    testImplementation(libs.assertj.core)

    testImplementation(libs.jackson.datatype.jdk8)
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.jackson.dataformat.yaml)
}
