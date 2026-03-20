// This file contains common conventions for Java code with no framework dependencies

// VT: FIXME: Using a hack from https://github.com/gradle/gradle/issues/15383
import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
    java
    jacoco
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--should-stop=ifError=FLOW")
}

tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.add("--should-stop=ifError=FLOW")
}

dependencies {

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)

    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.platform.launcher)
}
