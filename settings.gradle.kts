// External dependencies

include("submodules:automation-hat-driver")
include("submodules:owapi-reborn")
include("submodules:servomaster:servomaster-common")
include("submodules:xbee-api-reactive:xbee-api")

// Modules

include("modules:hcc-bootstrap")
include("modules:hcc-common")
include("modules:hcc-config")
include("modules:hcc-director")
include("modules:hcc-driver")
include("modules:hcc-ha")
include("modules:hcc-http")
include("modules:hcc-http-gae")
include("modules:hcc-influxdb")
include("modules:hcc-model")
include("modules:hcc-mqtt")
include("modules:hcc-raspberry-pi")
include("modules:hcc-owapi")
include("modules:hcc-scheduler")
include("modules:hcc-scheduler-gcal-v3")
include("modules:hcc-swing")
include("modules:hcc-webui")
include("modules:hcc-xbee")
include("modules:hcc-data-source-api")

// Run options

// One ring to find^Wrun them all

// This is the minimal application that reads the configuration from the
// given file, and contains no extra bells and whistles. May be ideal to run
// with stable and known good configurations, but may not be the best choice
// to experiment with.

include("app:hcc-minimal")

// This is the SpringBoot application, with all SpringBoot bells and
// whistles (including emitting metrics) included. See application.yaml for
// details.

// Run with:
//   ./gradlew bootRun --args='--spring.profiles.active=<your-profile>'

include("app:hcc-springboot")

// This is the Quarkus application, with all Quarkus bells and
// whistles (including emitting metrics) included. See application.yaml for
// details.

// Run with (for starters; native app instructions coming):
//   QUARKUS_PROFILE=<your-profile> ./gradlew quarkusDev

// VT: FIXME: temporarily removed to accommodate https://github.com/autonomousapps/dependency-analysis-gradle-plugin - they don't like each other, need to find a permanent fix
// include("app:hcc-quarkus")

rootProject.name = "dz3-master"
