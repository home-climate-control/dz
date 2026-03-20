plugins {
    id("hcc.java-common-conventions")
}

dependencies {

    implementation("com.homeclimatecontrol:automation-hat-driver")
    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-data-source-api"))
    implementation(project(":modules:hcc-model"))
}
