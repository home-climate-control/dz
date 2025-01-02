plugins {
    id("buildlogic.java-common-conventions")
}

dependencies {

    implementation(libs.influxdb)

    implementation(project(":modules:hcc-director"))
}
