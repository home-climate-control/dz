plugins {
    java
    `maven-publish`
    jacoco
    alias(libs.plugins.errorprone)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.git.properties) apply false

    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false

    alias(libs.plugins.quarkus.plugin) apply false

    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.gradle.dependency.analysis)
    alias(libs.plugins.gradle.doctor)
}

sonarqube {
    properties {
        property("sonar.projectKey", "home-climate-control_dz")
        property("sonar.organization", "home-climate-control")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

subprojects {

    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "jacoco")
    apply(plugin = rootProject.libs.plugins.errorprone.get().pluginId)

    group = "net.sf.dz3"
    version = "4.3.0-SNAPSHOT"

    jacoco {
        toolVersion = rootProject.libs.versions.jacoco.get()
    }

    tasks.compileJava {
        options.release = 17
    }

    tasks.test {
        finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test) // tests are required to run before generating the report
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    repositories {
        mavenCentral()
        mavenLocal()
    }

    dependencies {
        errorprone(rootProject.libs.errorprone)
    }

    tasks.test {
        useJUnitPlatform()
    }
}
