influx
==

Integration that allows to use [InfluxDB](https://www.influxdata.com/) as telemetry data sink.

> **NOTE:** This integration is different from default InfluxDB integration embedded into Spring and Quarkus, those must be configured in [infrastructure configuration](./index.md#infrastructure-specific).

Best explained by example:

```yaml
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

* `id`: Unique identifier this entity will be known as to the rest of the system.
* `instance`: Unique identifier for _this entity_ (not the whole system, like in [home-climate-control.instance](./home-climate-control.md)).
* `db`: InfluxDB database name
* `uri`: InfluxDB server to connect to
* `username`: InfluxDB username - optional, but recommended
* `password`: InfluxDB password - optional, but recommended
* `sensor-feed-mapping`: list of pairs where on the left is the [sensor](./sensors-and-switches.md#sensors) ID, and on the right is the name it will translate to in InfluxDB.

### Property of
* [connectors](./connectors.md)

---
[^^^ Configuration](./index.md)  
[^^^ connectors](./connectors.md)