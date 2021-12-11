plugins {
    id("java")
    id("java-library")
    id("maven-publish")
    id("jacoco")
    id("com.github.spotbugs")
    id("net.ltgt.errorprone")
    id("org.sonarqube")
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
        toolVersion = "0.8.6"
    }

    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

//    apply(plugin ="com.github.spotbugs")
//
//    spotbugs {
//
//        excludeFilter.set(file("$rootProject.projectDir/findbugs-exclude.xml"))
//        ignoreFailures.set(true)
//    }

    repositories {
        mavenCentral()
        mavenLocal()
    }

    dependencies {

        implementation("org.apache.logging.log4j:log4j-api:2.14.1")
        implementation("org.apache.logging.log4j:log4j-core:2.14.1")

        implementation("io.projectreactor:reactor-core:3.4.11")
        implementation("io.projectreactor:reactor-tools:3.4.11")

        testImplementation("org.mockito:mockito-core:3.11.2")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
        testImplementation("org.assertj:assertj-core:3.21.0")
        testImplementation("io.projectreactor:reactor-test:3.4.11")

        errorprone("com.google.errorprone:error_prone_core:2.10.0")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
