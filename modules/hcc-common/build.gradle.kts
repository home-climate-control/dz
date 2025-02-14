plugins {
    id("buildlogic.java-library-conventions")
}

dependencies {

    api(libs.log4j.api)
    api(libs.commons.lang3)
    api(libs.reactor.core)
}
