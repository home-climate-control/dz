plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // VT: NOTE: Search the codebase for import org.gradle.accessors.dm.LibrariesForLibs to see the rationale for this
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
