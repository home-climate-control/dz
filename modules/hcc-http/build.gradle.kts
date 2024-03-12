plugins {
    `java-library`
}

dependencies {

    api(libs.httpclient)

    api(project(":modules:hcc-director"))
}
