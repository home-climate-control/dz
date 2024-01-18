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
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "jacoco")
    apply(plugin = "net.ltgt.errorprone")

    group = "net.sf.dz3"
    version = "4.2.0"

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

        implementation(rootProject.libs.log4j.api)
        implementation(rootProject.libs.log4j.core)

        implementation(rootProject.libs.reactor.core)
        implementation(rootProject.libs.reactor.tools)

        testImplementation(rootProject.libs.mockito)
        testImplementation(rootProject.libs.junit5.api)
        testImplementation(rootProject.libs.junit5.params)
        testImplementation(rootProject.libs.assertj.core)
        testImplementation(rootProject.libs.reactor.test)

        testRuntimeOnly(rootProject.libs.junit5.engine)

        errorprone(rootProject.libs.errorprone)
    }

    tasks.test {
        useJUnitPlatform()
    }
}
