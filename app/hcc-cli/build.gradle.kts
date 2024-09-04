plugins {
    application

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

application {
    applicationName = "hcc-cli"
    mainClass.set("com.homeclimatecontrol.hcc.cli.HccCLI")
}

dependencies {

    implementation(libs.httpclient)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jcommander)
    implementation(libs.reactor.tools)
    implementation(libs.springboot.starter)
    implementation(libs.springboot.starter.log4j2)
    implementation(libs.springboot.starter.rsocket)

    implementation(project(":modules:hcc-common"))
    implementation(project(":modules:hcc-data-source-api"))
    implementation(project(":modules:hcc-model"))
}

configurations {
    all {
        exclude("org.springframework.boot", "spring-boot-starter-logging")
    }
}
