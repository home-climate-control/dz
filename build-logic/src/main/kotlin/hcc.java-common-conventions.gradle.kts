// This file contains common conventions for Java code with no framework dependencies

// VT: FIXME: Using a hack from https://github.com/gradle/gradle/issues/15383
import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
    java
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
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
