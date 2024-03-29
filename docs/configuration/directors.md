directors
==

Director is an entity that brings everything together. There is one director per one HVAC unit.

Best explained by example:

```yaml
  directors:
    - id: house-unit1
      connectors:
        - influxdb-connector-house
        - http-connector-appspot-v3
      sensor-feed-mapping:
        bedroom-nw-temperature: bedroom-nw
        bedroom-se-temperature: bedroom-se
      unit: unit1
      hvac: heatpump-unit1
      mode: heating
    - id: server-room
      connectors:
        - influxdb-connector-house
        - http-connector-appspot-v3
      sensor-feed-mapping:
        server-room-temperature: room-server
      unit: fan-panel
      hvac: fan-panel-server-room
      mode: cooling
```

* `id`: Unique identifier this entity will be known as to the rest of the system.
* `connectors`: List of references to [connectors](./connectors.md). Optional. Currently available connectors are:
  * [home-assistant](./home-assistant.md)
  * [http](./http.md)
  * [influx](./influx.md)
* `sensor-feed-mapping`: List of relations from the [sensor](./sensors-switches-fans.md) reference (on the left) to the [zone](./zones.md) it is serving (on the right).
* `unit`: [Unit abstraction](./units.md) to use.
* `hvac`: [HVAC device](./hvac.md) to use.
* `mode`: Initial mode for this director. Note that the configuration above shows different units in different modes (yes, this is supported).

### Property of
* [home-climate-control](./home-climate-control.md)

### Used in
* [web-ui](./web-ui.md)
* [console](./console.md)

---
[^^^ Configuration](./index.md)
