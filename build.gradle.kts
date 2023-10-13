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

val assertjVersion: String by project
val errorproneVersion: String by project
val log4jVersion: String by project
val mockitoVersion: String by project
val reactorVersion: String by project

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    group = "net.sf.dz3"
    version = "3.6.8-SNAPSHOT"

    jacoco {
        toolVersion = "0.8.9"
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

        implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
        implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")

        implementation("io.projectreactor:reactor-core:$reactorVersion")
        implementation("io.projectreactor:reactor-tools:$reactorVersion")

        testImplementation("org.mockito:mockito-core:$mockitoVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
        testImplementation("org.assertj:assertj-core:$assertjVersion")
        testImplementation("io.projectreactor:reactor-test:$reactorVersion")

        errorprone("com.google.errorprone:error_prone_core:$errorproneVersion")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
