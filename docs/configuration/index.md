Home Climate Control: Configuration Reference
==
## Variants & Infrastructure Specific
Read more [here](./variants.md).

### General Structure
Here's the configuration file skeleton. Order of these entries is important.
```yaml
home-climate-control:
  instance: ...
  esphome: ...
  zigbee2mqtt: ...
  zwave2mqtt: ...
  onewire: ...
  xbee: ...
  mocks: ...
  filters: ...
  hvac: ...
  zones: ...
  schedule: ...
  connectors: ...
  units: ...
  directors: ...
  web-ui: ...
  console: ...
```
### Details

* [home-climate-control](./home-climate-control.md)
    * [instance](./home-climate-control.md#instance)
    * [MQTT connectors](./mqtt.md)
        * [esphome](./esphome.md)
        * [zigbee2mqtt](./zigbee2mqtt.md)
        * [zwave2mqtt](./zwave2mqtt.md)
    * [onewire](./1-wire.md)
    * [xbee](./xbee.md)
    * [mocks](./mocks.md)
    * [filters](./filters.md)
    * [hvac](./hvac.md)
    * [zones](./zones.md)
    * [schedule](./schedule.md)
    * [connectors](./connectors.md)
        * [influx](./influx.md)
        * [http](./http.md)
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
    - broker:
        host: mqtt-esphome
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
  hvac:
    - heatpump:
        - id: heatpump-main
          switch-mode: switch-heatpump-mode
          switch-mode-reverse: true
          switch-running: switch-heatpump-running
          switch-fan: switch-heatpump-fan
          filter:
            lifetime: 200H
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
---
[^^^ Index](../index.md)
