plugins {
    id("buildlogic.java-library-conventions")
}

dependencies {

    api(project(":modules:hcc-common"))
    api(project(":modules:hcc-model"))
    api(project(":modules:hcc-scheduler"))
}
