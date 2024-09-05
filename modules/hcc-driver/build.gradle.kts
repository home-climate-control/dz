plugins {
    `java-library`
}

dependencies {

    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-data-source-api"))
    api(project(":modules:hcc-model"))
}
