web-ui
==

Browser accessible representation of the system.

Best explained by example:

```yaml
  web-ui:
    port: 9999
    directors:
      - house-unit1
      - house-unit2
      - server-room
    sensors:
      - ambient-courtyard-temperature
      - air-ambient-northeast
      - ambient-patio-temperature
```

* `port`: Port to listen on. Defaults to 3939.
* `directors`: Set of references to [directors](./directors.md).
* `sensors`: Set of references to [sensors](./sensors-and-switches.md).

In addition to entities above, contains the [instrument cluster](../instrument-cluster/index.md), collected implicitly from all essential system components.

### Property of
* [home-climate-control](./home-climate-control.md)

### Related to
* [console](./console.md)

---
[^^^ Configuration](./index.md)
