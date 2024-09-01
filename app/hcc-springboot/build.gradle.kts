import com.google.cloud.tools.jib.gradle.extension.layerfilter.Configuration

buildscript {
    dependencies {
        classpath(libs.jib.layer.filter)
    }
}

plugins {
    application
    `jacoco-report-aggregation`
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    alias(libs.plugins.git.properties)

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)

    alias(libs.plugins.jib)
}

application {
    applicationName = "hcc"
    mainClass.set("net.sf.dz3r.runtime.springboot.HccApplication")
}

dependencies {
    implementation(project(":modules:hcc-bootstrap"))
}

// SpringBoot additions
dependencies {

    annotationProcessor(libs.springboot.configuration.processor)

    implementation(libs.springboot.starter.actuator)
    implementation(libs.springboot.starter.log4j2)
    implementation(libs.springboot.starter.rsocket)
    implementation(libs.springboot.starter.webflux)

    testImplementation(libs.springboot.starter.test)

    runtimeOnly(libs.micrometer.registry.influx)
    runtimeOnly(libs.micrometer.registry.jmx)
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

        // Uncomment this block to execute the jibDockerBuild task on a Pi.
        // Regrettably, multiplatform build to Docker is not supported.
        // See for more: https://github.com/GoogleContainerTools/jib/issues/2743

        // platforms {
        //     platform {
        //         architecture = "arm64"
        //         os = "linux"
        //     }
        // }
    }

    to {
        // Final name when the dust settles: "climategadgets/home-climate-control-springboot
        image = "climategadgets/hcc-springboot-experimental"
    }

    pluginExtensions {
        pluginExtension {
            implementation = "com.google.cloud.tools.jib.gradle.extension.layerfilter.JibLayerFilterExtension"

            configuration(Action<Configuration> {
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
        // VT: FIXME: Hack to see if there is thread starvation when running on boxes with few cores
        jvmFlags =listOf("-Dreactor.schedulers.defaultPoolSize=20", "-Dreactor.schedulers.defaultBoundedElasticSize=200")
        // Whatever profiles that are provided on the command line will be added to this one
        args = listOf("--spring.profiles.active=docker")
        workingDirectory = "${jib.container.appRoot}/app/"
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
