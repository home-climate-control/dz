val jacksonVersion: String by project
val jmdnsVersion: String by project
val springStandaloneVersion: String by project

dependencies {

    implementation("org.springframework:spring-context:$springStandaloneVersion")
    implementation("org.springframework:spring-webflux:$springStandaloneVersion")
    implementation("io.projectreactor.netty:reactor-netty:1.1.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.jmdns:jmdns:$jmdnsVersion")

    implementation(project(":dz3r-config"))
    implementation(project(":dz3r-director"))
}
