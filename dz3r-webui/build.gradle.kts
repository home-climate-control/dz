dependencies {

    implementation("org.springframework:spring-context:5.3.19")
    implementation("org.springframework:spring-webflux:5.3.19")
    implementation("io.projectreactor.netty:reactor-netty:1.0.7")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.3")

    implementation(project(":dz3r-director"))
}
