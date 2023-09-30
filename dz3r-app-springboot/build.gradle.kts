buildscript {
    dependencies {
        classpath("com.google.cloud.tools:jib-layer-filter-extension-gradle:0.3.0")
    }
}

plugins {
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    id("com.gorylenko.gradle-git-properties")

    id("org.springframework.boot")
    id("io.spring.dependency-management")

    id("com.google.cloud.tools.jib") version "3.3.2"
}

val reactorVersion: String by project

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
    testImplementation("io.projectreactor:reactor-test:$reactorVersion")
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

    pluginExtensions {
        pluginExtension {
            implementation = "com.google.cloud.tools.jib.gradle.extension.layerfilter.JibLayerFilterExtension"
            configuration(Action<com.google.cloud.tools.jib.gradle.extension.layerfilter.Configuration> {
                filters {
                    filter {
                        // Filter out all custom configurations that may be present in the source tree protected by .gitignore
                        glob = "**/application-*.yaml"
                    }
                    filter {
                        // ...but retain the Docker specific configuration
                        glob = "**/application-docker.yaml"
                        toLayer = "Docker profile"
                    }
                }
            })
        }
    }

    container {
        // Whatever profiles that are provided on the command line will be added to this one
        args = listOf("--spring.profiles.active=docker")
        workingDirectory = "${jib.container.appRoot}/app/"
    }

}
