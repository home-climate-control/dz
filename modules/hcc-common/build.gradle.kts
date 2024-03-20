plugins {
    `java-library`
}

dependencies {

    api(libs.log4j.api)
    api(libs.commons.lang3)
    api(libs.reactor.core)

    testImplementation(libs.junit5.params)
    testImplementation(libs.assertj.core)
    testImplementation(libs.reactor.test)
}
