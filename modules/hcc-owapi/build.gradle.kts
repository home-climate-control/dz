plugins {
    id("hcc.java-common-conventions")
    id("hcc.reactor-conventions")
}

dependencies {

    implementation(libs.rxtx)

    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-data-source-api"))
    implementation(project(":modules:hcc-model"))
    implementation(project(":modules:hcc-driver"))
    implementation("com.homeclimatecontrol:owapi-reborn")
}
