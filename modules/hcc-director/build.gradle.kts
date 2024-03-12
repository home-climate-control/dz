plugins {
    `java-library`
}

dependencies {

    api(project(":modules:hcc-common"))
    api(project(":modules:hcc-model"))
    api(project(":modules:hcc-scheduler"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.api)
}
