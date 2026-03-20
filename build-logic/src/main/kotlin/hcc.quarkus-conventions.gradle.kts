// Conventions specific for modules with Project Reactor dependencies

// VT: FIXME: Using a hack from https://github.com/gradle/gradle/issues/15383
import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
    java
}

dependencies {

    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.config.yaml)
    implementation(libs.quarkus.rest)
    testImplementation(libs.quarkus.junit5)
}
