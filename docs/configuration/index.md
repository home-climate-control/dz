Home Climate Control: Configuration Reference
==
## Variants
There are currently four ways to run the application (see [build](../build/index.md) for details):
* Standalone ("minimal")
* SpringBoot
* Quarkus
* Docker

The "minimal" variant is the only one that requires things to be spelled out exactly, the rest follow the usual Spring and Quarkus conventions (splitting configuration into many files, specifying active profiles, using YAML anchors, etc.).

### YAML Anchors

Configuration file for HCC can be quite big (400+ lines is not unheard of), do take advantage of [YAML Anchors](https://yaml.org/spec/1.2.2/#3222-anchors-and-aliases) to make it stable.

## Infrastructure Specific
This part contains what you would usually provide for your standard Spring or Quarkus applications. For your convenience, basic
[localhost](../../dz3r-app-springboot/src/main/resources/application-localhost.yaml) and
[docker](../../dz3r-app-springboot/src/main/resources/application-docker.yaml) profiles are included into default configuration - feel free to override them as you see fit.
## Home Climate Control Specific

> **NOTE:** There are two source trees, [Spring specific](../../dz3r-app-springboot/src/main/java/net/sf/dz3/runtime/config/HccRawRecordConfig.java) and
> [Quarkus specific](../../dz3r-app-quarkus/src/main/java/net/sf/dz3/runtime/config/quarkus/HccRawInterfaceConfig.java) that directly map to configuration entries.
> They are expected to behave the same way (the reason two of them exist is very different conventions followed by Spring and Quarkus), if there's ever any discrepancy (the application works invoked with Spring and doesn't work invoked with Quarkus, or the other way around), please [file an issue](https://github.com/home-climate-control/dz/issues).

> **NOTE:** You can use your IDE to edit configuration files, this will give you code completion and extended documentation, thanks to [Spring Configuration Metadata](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html), and "code walking" if you are using YAML anchors.

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

### Example Configuration

No-frills, one heatpump, three zones configuration. With traces of some lessons learned, though - note the naming conventions. This system grows much faster than one would expect.

> **NOTE:** Calendar integration turned out to be one of the most usable features. Take your time to configure it.

```yaml
home-climate-control:
  instance: bootstrap
  esphome:
    - host: mqtt-esphome
      root-topic: /esphome
      sensors:
        - address: air-ambient-north
        - address: air-ambient-south
        - address: air-bedroom-master
        - address: air-bedroom-kids
        - address: air-family-room
      switches:
        - id: switch-heatpump-mode
          address: /esphome/board-MAC/switch-heatpump-mode
        - id: switch-heatpump-running
          address: /esphome/board-MAC/switch-heatpump-running
        - id: switch-heatpump-fan
          address: /esphome/board-MAC/switch-heatpump-fan
  zones:
    - id: bedroom-master
      name: Master Bedroom
      controller:
        p: 0.7
        i: 0.000002
        limit: 1.1
      settings:
        setpoint: 31
        setpoint-range:
          min: 25.5
          max: 33
    - id: bedroom-kids
      name: Kids' Bedroom
      controller:
        p: 1
        i: 0.000002
        limit: 1.1
      settings:
        setpoint: 31
        setpoint-range:
          min: 25.5
          max: 33
    - id: room-family
      name: Family Room
      controller:
        p: 0.85
        i: 0.0000008
        limit: 1.1
      settings:
        setpoint: 31
        setpoint-range:
          min: 26
          max: 33
  schedule:
    google-calendar:
      - zone: bedroom-master
        calendar: "HCC Schedule: Master Bedroom"
      - zone: bedroom-kids
        calendar: "HCC Schedule: Kids' Bedroom"
      - zone: room-family
        calendar: "HCC Schedule: Family Room"
  connectors:
    - influx:
        id: influxdb-connector-house
        instance: house
        db: hcc
        uri: http://dx:8086
        sensor-feed-mapping:
          air-ambient: air-ambient-temperature
          air-bedroom-master: air-bedroom-master-temperature
          air-bedroom-kids: air-bedroom-kids-temperature
          air-family-room: air-family-room-temperature
  hvac:
    - heatpump:
        - id: heatpump-main
          switch-mode: switch-heatpump-mode
          switch-mode-reverse: true
          switch-running: switch-heatpump-running
          switch-fan: switch-heatpump-fan
          filter:
            lifetime: 200H
  units:
    - single-stage:
        - id: heatpump
  directors:
    - id: house
      connectors:
        - influxdb-connector-house
      sensor-feed-mapping:
        air-bedroom-master: bedroom-master
        air-bedroom-kids: bedroom-kids
        air-family-room: room-family
      unit: heatpump
      hvac: heatpump-main
      mode: cooling
  web-ui:
    directors:
      - house
    sensors:
      - air-ambient-north
      - air-ambient-south
  console:
    directors:
      - house
    sensors:
      - air-ambient-north
      - air-ambient-south
```
