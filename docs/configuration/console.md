console
==

GUI representation of the system.

Best explained by example:

```yaml
  console:
    directors:
      - house-unit1
      - house-unit2
      - server-room
    sensors:
      - ambient-courtyard-temperature
      - air-ambient-northeast
      - ambient-patio-temperature
```

* `directors`: Set of references to [directors](./directors.md).
* `sensors`: Set of references to [sensors](./sensors-and-switches.md).

In addition to entities above, contains the [instrument cluster](../instrument-cluster/index.md), collected implicitly from all essential system components.

### Visualization Details

Coming up. For now, please see [https://www.homeclimatecontrol.com/hcc-core](https://www.homeclimatecontrol.com/hcc-core).

### Property of
* [home-climate-control](./home-climate-control.md)

### Related to
* [web-ui](./web-ui.md)

---
[^^^ Configuration](./index.md)
