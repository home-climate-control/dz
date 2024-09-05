dependencies {

    implementation(libs.rxtx)

    implementation("com.homeclimatecontrol:owapi-reborn")
    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-data-source-api"))
    implementation(project(":modules:hcc-model"))
    implementation(project(":modules:hcc-driver"))

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.api)
    testImplementation(libs.reactor.test)
}
