subprojects {
    dependencies {
        testRuntimeOnly(rootProject.libs.junit5.engine)
    }
}

dependencies {
    runtimeOnly("javax.xml.bind:jaxb-api:2.3.1")
}
