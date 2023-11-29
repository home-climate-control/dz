web-ui
==

Browser accessible representation of the system.

Best explained by example:

```yaml
  web-ui:
    http-port: 6666
    duplex-port: 7777
    directors:
      - house-unit1
      - house-unit2
      - server-room
```

### http-port
HTTP port to listen on. Defaults to 3939.

### duplex-port
Duplex protocol port to listen on. Defaults to 3940.

### interface
Interfaces to listen on. Defaults to `0.0.0.0`.

### directors
Set of references to [directors](./directors.md). If you skip it HCC will assume you want all the directors.

> **NOTE**: Be careful with an empty set. All configuration readers may skip a bare `web-ui:` or `webui.directors:` as absent.
> Both "include all" and "missing" will be logged at `WARNING` level, verify if what you think you configured is what HCC thinks it is.
> It is best to include at least one non-empty keyword (in this case, `port`).

### sensors
Unlike the [console](./console.md#sensors), WebUI takes all the configured [sensors](./sensors-switches-fans.md) as a part of the implied configuration.

### units

Specifies the _initial_ temperature measurement units. Defaults to C&deg;. Note that this only applies to [zone](./zones.md) display, [instrument cluster](../instrument-cluster/index.md) will display values in [SI units](https://en.wikipedia.org/wiki/International_System_of_Units).

### Implied Configuration

In addition to entities above, contains the [instrument cluster](../instrument-cluster/index.md), collected implicitly from all essential system components.

### Property of
* [home-climate-control](./home-climate-control.md)

### Related to
* [console](./console.md)

---
[^^^ Configuration](./index.md)
