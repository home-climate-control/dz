plugins {
    `java-library`
}

dependencies {

    implementation(libs.jackson.databind)

    implementation(project(":modules:hcc-common"))
    api(project(":modules:hcc-http"))
}
