plugins {
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    id("com.gorylenko.gradle-git-properties")

    id("org.springframework.boot")
    id("io.spring.dependency-management")

    id("com.google.cloud.tools.jib") version "3.3.2"
}

dependencies {

    implementation(project(":dz3r-bootstrap"))

    // SpringBoot additions
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    runtimeOnly("io.micrometer:micrometer-registry-influx")
    runtimeOnly("io.micrometer:micrometer-registry-jmx")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}
configurations {
    all {
        exclude("org.springframework.boot", "spring-boot-starter-logging")
    }
}

jib {

    from {
        // JDK, not JRE (which is the default). Necessary to support sane exception traces for Project Reactor.
        image = "eclipse-temurin:17-jdk"
    }

    to {
        // Final name when the dust settles: "climategadgets/home-climate-control-springboot
        image = "climategadgets/hcc-springboot-experimental"
    }

    container {
        // Whatever profiles that are provided on the command line will be added to this one
        args = listOf("--spring.profiles.active=docker")
        workingDirectory = "${jib.container.appRoot}/app/"
    }
}
