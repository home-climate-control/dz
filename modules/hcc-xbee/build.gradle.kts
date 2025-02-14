plugins {
    id("buildlogic.java-common-conventions")
}

dependencies {

    implementation("com.homeclimatecontrol:xbee-api")
    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-data-source-api"))
    implementation(project(":modules:hcc-driver"))
}
