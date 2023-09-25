onewire
==

Integration with [Dallas Semiconductors 1-Wire wired half duplex serial bus](https://en.wikipedia.org/wiki/1-Wire).

Best explained by example:

```yaml
  onewire:
    - serial-port: /dev/ttyUSB0
      sensors:
        - id: ambient-garage
          address: 6D00080021DF5010
        - id: server-room-temperature
          address: 9C0000027CAEEA28
        - id: west-1wire-temperature
          address: 0500080021C9B810
```

### sensors
* `id`: Unique identifier this sensor will be known as to the rest of the system.
* `address`: 1-Wire hardware address of the sensor.

The only sensors supported by HCC are temperature sensors, so there is no `measurement` property for these.

### switches

Stay tuned.

### Property of
* [home-climate-control](./home-climate-control.md)

---
[^^^ Configuration](./index.md)
