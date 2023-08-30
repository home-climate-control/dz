Home Climate Control: Configuration
==
## Variants
There are currently four ways to run the application (see [build](../build/index.md) for details):
* Standalone ("minimal")
* SpringBoot
* Quarkus
* Docker

The "minimal" variant is the only one that requires things to be spelled out exactly, the rest follow the usual Spring and Quarkus conventions (splitting configuration into many files, specifying active profiles, using YAML anchors, etc.).

## Infrastructure Specific
This part contains what you would usually provide for your standard Spring or Quarkus applications. For your convenience, basic
[localhost](../../dz3r-app-springboot/src/main/resources/application-localhost.yaml) and
[docker](../../dz3r-app-springboot/src/main/resources/application-docker.yaml) profiles are included into default configuration - feel free to override them as you see fit.
## Home Climate Control Specific
### General Conventions
There are two source trees, [Spring specific](../../dz3r-app-springboot/src/main/java/net/sf/dz3/runtime/config/HccRawRecordConfig.java) and
[Quarkus specific](../../dz3r-app-quarkus/src/main/java/net/sf/dz3/runtime/config/quarkus/HccRawInterfaceConfig.java) that directly map to configuration entries.
They are expected to be the same (the reason two of them exist is very different conventions followed by Spring and Quarkus), if there's ever any discrepancy (the application works invoked with Spring and doesn't work invoked with Quarkus, or the other way around), please [file an issue](https://github.com/home-climate-control/dz/issues).

> **NOTE:** You can use your IDE to edit configuration files, this will give you code completion and extended documentation, thanks to [Spring Configuration Metadata](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html).

### General Structure
Here's the configuration file skeleton:
```yaml
home-climate-control:
  instance: <your-instance-name>
  esphome: ...
  zigbee2mqtt: ...
  zwave2mqtt: ...
  onewire: ...
  xbee: ...
  mocks: ...
  filters: ...
  zones: ...
  schedule: ...
  connectors: ...
  hvac: ...
  units: ...
  directors: ...
  web-ui: ...
  console: ...
```
### Details

**FAIR WARNING:** work in progress, details may be missing. Please rely on code assist until all the docs are written.

* [home-climate-control](./home-climate-control.md)
  * [MQTT connectors](./mqtt.md)
    * [esphome](./esphome.md)
    * [zigbee2mqtt](./zigbee2mqtt.md)
    * [zwave2mqtt](./zwave2mqtt.md)
  * [onewire](./1-wire.md)
  * [xbee](./xbee.md)
  * [mocks](./mocks.md)
  * [filters](./filters.md)
  * [zones](./zones.md)
  * [schedule](./schedule.md)
  * [connectors](./connectors.md)
    * [influx](./influx.md)
    * [http](./http.md)
  * [hvac](./hvac.md)
  * [units](./units.md)
  * [directors](./directors.md)
  * [web-ui](./web-ui.md)
  * [console](./console.md)
