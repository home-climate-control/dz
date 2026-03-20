plugins {
    id("hcc.java-common-conventions")
    id("hcc.reactor-conventions")
}

dependencies {

    implementation(libs.influxdb)

    implementation(project(":modules:hcc-director"))
}
