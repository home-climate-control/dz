plugins {
    id("hcc.java-library-conventions")
}

dependencies {

    api(project(":modules:hcc-model"))
    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-data-source-api"))
}
