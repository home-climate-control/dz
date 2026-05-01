plugins {
    id("hcc.java-library-conventions")
}

dependencies {

    api(project(":modules:hcc-http"))
    implementation(libs.jackson.databind)

    implementation(libs.httpclient5)
    implementation(project(":modules:hcc-common"))
}
