influx
==

Integration that allows to use [InfluxDB 1.x](https://www.influxdata.com/time-series-platform/) as telemetry data sink.

InfluxDB 2.x is not yet supported, [vote or submit a PR](https://github.com/home-climate-control/dz/issues/197) if you want to use 2.x instead or in addition to 1.x. 

> **NOTE:** This integration will be inactive except for `sensor-field-mapping` unless included into [directors.connectors](./directors.md).   

> **NOTE:** This integration is different from default InfluxDB integration embedded into Spring and Quarkus, those must be configured in [infrastructure configuration](./index.md#infrastructure-specific).

### Telemetry Emitted

Short list:

* Zone state (setpoints, PID controller internal state, and more)
* Economizer state (setpoints, PID controller internal state, and more)
* Unit state (demand, uptime, filter life left, and more)

### Configuration

Best explained by example:

```yaml
  connectors:
    - influx:
        id: influxdb-connector-house
        instance: house
        db: dz-reactive
        uri: http://dx:8086
        username: <InfluxDB username>
        password: <InfluxDB password>
        sensor-feed-mapping:
          ambient-courtyard-temperature: air-ambient-courtyard
          air-ambient-northeast: air-ambient-northeast
```

* `id`: Unique identifier this entity will be known as to the rest of the system. In particular, it is used by [directors.connectors](./directors.md).
* `instance`: Unique identifier for _this entity_ (not the whole system, like in [home-climate-control.instance](./home-climate-control.md#instance) - though that value can be used if there is just one InfluxDB connector).
* `db`: InfluxDB database name
* `uri`: InfluxDB server to connect to
* `username`: InfluxDB username - optional, but recommended
* `password`: InfluxDB password - optional, but recommended
* `sensor-feed-mapping`: Optional list of pairs where on the left is the [sensor](./sensors-switches-fans.md#sensors) ID, and on the right is the name it will translate to in InfluxDB. Note that this mapping is on top of the standard telemetry emitted for all the entities included in the [directors](./directors.md) this connector is attached to.

### Property of
* [connectors](./connectors.md)

---
[^^^ Configuration](./index.md)  
[^^^ connectors](./connectors.md)
