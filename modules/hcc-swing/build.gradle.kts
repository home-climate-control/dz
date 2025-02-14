plugins {
    id("buildlogic.java-common-conventions")
}

dependencies {

    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-config"))
    implementation(project(":modules:hcc-director"))
}
