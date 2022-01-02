plugins {
    `version-catalog`
    `java`
    `java-library`
    `maven-publish`
    jacoco
    id("com.github.spotbugs")
    id("net.ltgt.errorprone")
    id("org.sonarqube")
    id("com.autonomousapps.dependency-analysis")
}

sonarqube {
    properties {
        property("sonar.projectKey", "home-climate-control_dz")
        property("sonar.organization", "home-climate-control")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.reportPaths", "build/jacoco/test.exec")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
    }
}

subprojects {

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "jacoco")
    apply(plugin = "net.ltgt.errorprone")

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    group = "net.sf.dz3"
    version = "3.6.8-SNAPSHOT"

    jacoco {
        toolVersion = "0.8.6"
    }

    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    // VT: NOTE: This plugin is unforgivably slow (adds well over a minute to build time),
    // need to invoke it conditionally
    /*
    apply(plugin ="com.github.spotbugs")

    spotbugs {
        excludeFilter.set(rootProject.file("findbugs-exclude.xml"))
        ignoreFailures.set(true)
    }
    */

    repositories {
        mavenCentral()
        mavenLocal()
    }

    dependencies {

        constraints {
            implementation("org.apache.logging.log4j:log4j-core") {
                version {
                    strictly("[2.17, 3[")
                    prefer("2.17.0")
                }
                because("CVE-2021-44228, CVE-2021-45046, CVE-2021-45105: Log4j vulnerable to remote code execution and other critical security vulnerabilities")
            }
        }

        testImplementation(rootProject.libs.log4j2.api)
        implementation(rootProject.libs.log4j2.core)

        implementation(rootProject.libs.reactor.core)
        implementation(rootProject.libs.reactor.tools)

        testImplementation(rootProject.libs.mockito)
        testImplementation(rootProject.libs.junit.jupiter.api)
        testImplementation(rootProject.libs.junit.jupiter.params)
        testRuntimeOnly(rootProject.libs.junit.jupiter.engine)
        testImplementation(rootProject.libs.assertj)
        testImplementation(rootProject.libs.reactor.test)

        errorprone(rootProject.libs.errorprone)
    }

    tasks.test {
        useJUnitPlatform()
    }
}
