onewire
==

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

### Property of
* [home-climate-control](./home-climate-control.md)

---
[^^^ Configuration](./index.md)
