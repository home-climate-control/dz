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

### `directors`
Set of references to [directors](./directors.md). If you skip it HCC will assume you want all the directors.

> **NOTE**: Be careful with an empty set. All configuration readers may skip a bare `console:` or `console.directors:` as absent.
> Both "include all" and "missing" will be logged at `WARNING` level, verify if what you think you configured is what HCC thinks it is.

### `sensors`
Set of references to [sensors](./sensors-and-switches.md).

> **NOTE**: Be careful with an empty set. Quarkus will not ignore it, but Spring will include all, and you likely have A LOT of sensors in the system, and it's unlikely that you want all of them on the console.
> Be sure to examine the boot log to verify how exactly the configuration was interpreted.

### Implied Configuration

In addition to entities above, contains the [instrument cluster](../instrument-cluster/index.md), collected implicitly from all essential system components.

### Visualization Details

Coming up. For now, please see [https://www.homeclimatecontrol.com/hcc-core](https://www.homeclimatecontrol.com/hcc-core).

### Property of
* [home-climate-control](./home-climate-control.md)

### Related to
* [web-ui](./web-ui.md)

---
[^^^ Configuration](./index.md)
