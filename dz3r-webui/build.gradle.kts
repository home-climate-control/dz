dependencies {

    implementation("org.springframework:spring-context:5.3.28")
    implementation("org.springframework:spring-webflux:5.3.28")
    implementation("io.projectreactor.netty:reactor-netty:1.1.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.7.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.0")

    implementation(project(":dz3r-director"))
}
