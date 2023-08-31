directors
==

Best explained by example:

Director is an entity that brings everything together.

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

* `id`: Unique director identifier.
* `connectors`: List of references to [connectors](./connectors.md). Optional.
* `sensor-feed-mapping`: List of relation from the sensor reference (on the left) to the zone it is serving (on the right).
* `unit`: Unit abstraction to use.
* `hvac`: HVAC device to use.
* `mode`: Initial mode for this director. Note that the configuration above shows different units in different modes (yes, this is supported).

### Property of
* [home-climate-control](./home-climate-control.md)

### Used in
* [web-ui](./web-ui.md)
* [console](./console.md)

---
[^^^ Configuration](./index.md)
