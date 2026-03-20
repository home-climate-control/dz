plugins {

    alias(libs.plugins.errorprone)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.git.properties) apply false

    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false

    // These two go together; "java" can't be removed without Quarkus failing
    java
    alias(libs.plugins.quarkus.plugin) apply false

    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.gradle.dependency.analysis)
    alias(libs.plugins.gradle.doctor)
}

sonarqube {
    properties {
        property("sonar.projectKey", "home-climate-control_dz")
        property("sonar.organization", "home-climate-control")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

doctor {
    javaHome {
        // Build breaks in IntelliJ IDEA on macOS even if JAVA_HOME is set correctly
        // (it picks up JetBrains JDK instead)
        failOnError.set(false)
    }
}

subprojects {

    apply(plugin = rootProject.libs.plugins.errorprone.get().pluginId)

    dependencies {
        errorprone(rootProject.libs.errorprone)
    }
}
