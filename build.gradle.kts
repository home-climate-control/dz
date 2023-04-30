plugins {
    java
    id("maven-publish")
    jacoco
    id("com.github.spotbugs")
    id("net.ltgt.errorprone")
    id("org.sonarqube")
    id("com.gorylenko.gradle-git-properties") apply false
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

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    group = "net.sf.dz3"
    version = "3.6.8-SNAPSHOT"



    jacoco {
        toolVersion = "0.8.8"
    }

    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

/*
    apply plugin: "findbugs"

    findbugs {

        excludeFilter = file("$rootProject.projectDir/findbugs-exclude.xml")
        ignoreFailures = true
    }
 */

    repositories {

        mavenCentral()
        mavenLocal()
    }

    dependencies {

        implementation("org.apache.logging.log4j:log4j-api:2.20.0")

        implementation("org.apache.logging.log4j:log4j-core:2.20.0")

        implementation("io.projectreactor:reactor-core:3.5.5")
        implementation("io.projectreactor:reactor-tools:3.5.5")

        testImplementation("org.mockito:mockito-core:3.11.2")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
        testImplementation("org.assertj:assertj-core:3.24.2")
        testImplementation("io.projectreactor:reactor-test:3.5.5")

        errorprone("com.google.errorprone:error_prone_core:2.18.0")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
