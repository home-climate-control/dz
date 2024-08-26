dependencies {

    implementation(libs.spring.standalone.context)
    implementation(libs.spring.standalone.webflux)
    implementation(libs.reactor.netty)
    implementation(libs.rsocket.core)
    implementation(libs.rsocket.transport.netty)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jmdns)

    implementation(project(":modules:hcc-data-source-api"))
    implementation(project(":modules:hcc-config"))
    implementation(project(":modules:hcc-director"))
}
