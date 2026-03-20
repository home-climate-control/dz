plugins {
    id("hcc.java-common-conventions")
    id("hcc.reactor-conventions")
}

dependencies {

    implementation("com.homeclimatecontrol:xbee-api")
    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-data-source-api"))
    implementation(project(":modules:hcc-driver"))
}
