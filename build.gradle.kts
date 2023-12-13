plugins {
    java
    id("maven-publish")
    jacoco
    id("net.ltgt.errorprone")
    id("org.sonarqube")
    id("com.gorylenko.gradle-git-properties") apply false

    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false

    id("io.quarkus") apply false

    id("com.github.ben-manes.versions")
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
    version = "4.2.0-SNAPSHOT"

    jacoco {
        toolVersion = "0.8.11"
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
        testRuntimeOnly(rootProject.libs.junit5.engine)
        testImplementation(rootProject.libs.assertj.core)
        testImplementation(rootProject.libs.reactor.test)

        errorprone(rootProject.libs.errorprone)
    }

    tasks.test {
        useJUnitPlatform()
    }
}
