val influxdbVersion: String by project

dependencies {

    implementation("org.influxdb:influxdb-java:$influxdbVersion")

    implementation(project(":dz3r-director"))
}
