dependencies {

    implementation(project(":dz3r-model"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    testImplementation("org.assertj:assertj-core:3.21.0")

    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.15.2")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
}
