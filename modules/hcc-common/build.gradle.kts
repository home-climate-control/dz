plugins {
    id("hcc.java-library-conventions")
    id("hcc.reactor-conventions")
}

dependencies {

    api(libs.log4j.api)
    api(libs.commons.lang3)
    api(libs.reactor.core)

    implementation(libs.log4j.core)
}
