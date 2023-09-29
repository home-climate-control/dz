plugins {
    // See https://github.com/home-climate-control/dz/issues/230
    // Should that bug be fixed, this goes to the parent
    id("com.gorylenko.gradle-git-properties")

    java
}

dependencies {

    api(project(":dz3r-director"))
    api(project(":dz3r-mqtt"))

    testImplementation("org.assertj:assertj-core:3.21.0")
}
