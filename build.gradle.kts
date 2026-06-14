plugins {

    alias(libs.plugins.errorprone)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.git.properties) apply false

    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false

    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.gradle.dependency.analysis)
    alias(libs.plugins.gradle.doctor)
    alias(libs.plugins.openrewrite.rewrite)
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

rewrite {
    // VT: NOTE: Do not run this recipe without reviewing results;
    // it silently misplaces comments in build files with SortDependencies
    // activeRecipe("org.openrewrite.gradle.GradleBestPractices")

    // An attempt to run this directly causes version catalog not to be updated
    // thus failing the rewrite (https://github.com/openrewrite/rewrite/issues/4852)
    // but let's leave a record of what we were trying to do, and fix the version catalog by hand
    activeRecipe("org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0")
    isExportDatatables = true
}

subprojects {

    apply(plugin = rootProject.libs.plugins.errorprone.get().pluginId)

    dependencies {
        errorprone(rootProject.libs.errorprone)
    }
}

dependencies {
    rewrite(libs.rewrite.spring)
}
