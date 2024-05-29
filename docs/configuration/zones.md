zones
==

Best explained by example:

```yaml
  zones:
    - id: &bedroom-master bedroom-master
      name: Master Bedroom
      controller:
        p: 0.7
        i: 0.000002
        limit: 1.1
      settings:
        setpoint: 31
        setpoint-range:
          min: 25.5
          max: 33
    - id: workshop
      name: Workshop
      controller:
        p: 1
        i: 0.000004
        limit: 1.1
      settings:
        setpoint: 28
        setpoint-range:
          min: 25
          max: 32
      economizer:
        ambient-sensor: ambient-patio-temperature
        controller:
          p: 1
          i: 0.0000008
          limit: 0.7
        mode: cooling
        hvac-device: economizer-a6
        timeout: 75S
        settings:
            changeover-delta: 1
            target-temperature: 22
            keep-hvac-on: true
```

### id
Unique identifier this zone will be known to the system as. Using a YAML anchor here (as in the first entry above) would be a good idea.

Currently, this identifier is used in:
* [directors](./directors.md)
* [home-assistant](./home-assistant.md)
* [http](./http.md)
* [influx](./influx.md)
* [schedule](./schedule.md)

### name
Human readable zone name

### controller
Zone [PID controller](https://en.wikipedia.org/wiki/Proportional%E2%80%93integral%E2%80%93derivative_controller). `p` and `i` are self-explanatory, `limit` is the [integral windup](https://en.wikipedia.org/wiki/Integral_windup) saturation limit. Values in the snippet above are good defaults. Looking at instrumentation (article coming) will help you to determine the right values for your zones.

Positive `p` and `i` values are used for cooling mode, negative for heating. `limit` stays the same for both.

> **NOTE:** this configuration item is the most important for your comfort. A more detailed explanation is coming, for now - step carefully here, especially with the `i` value.

### settings

* `setpoint` defines the default zone setpoint on system start (later overridden by [schedule](./schedule.md))
* `setpoint-range` defines the minimum and maximum allowable setpoint values for this zone.

### economizer

Optional.

> **NOTE:** Economizers are per-zone devices. It is quite possible to have multiple economizers for zones served by the same HVAC unit.

Cooling mode assumed:

* `ambient-sensor`: the sensor used as ambient for this economizer. Zones at different sides of the house may have different ambient temperatures (determined by sun exposure, shade, wind, presence of water, and a zillion of other factors), don't neglect this.
* `controller`: just like the zone configuration above.
* `mode`: self-explanatory
* `hvac-device`: at this point, the economizer is an on/off device (multistage coming). This is the identifier of the [HVAC device](./hvac.md) acting as an economizer.
* `timeout`: treat both indoor and ambient sensors as stale and shut off the economizer after not receiving data from them for this long. Default is 90 seconds. The system will complain at `INFO` level if this is happening.
* `settings`: This section is optional. If missing, it is assumed that the [schedule](./schedule.md) will take care of configuring economizer settings dynamically.
  * `changeover-delta`: ambient temperature has to be this much lower than indoors for the economizer to start working (subject to PID configuration). Surprisingly, a value of 0 works well due to [PID control magic](https://en.wikipedia.org/wiki/Proportional%E2%80%93integral%E2%80%93derivative_controller).
  * `target-temperature`: shut the economizer off when indoor temperature drops to this value.
  * `keep-hvac-on`: set to `true` if you want the main HVAC to be still working when the economizer is active (maximum comfort), and to `false` if you want to stop it (maximum cost savings).

#### controller
Economizer PID controller. `p` and `i` values are always positive, regardless of the mode.

### Property of
* [home-climate-control](./home-climate-control.md)

### Used in
* [schedule](./schedule.md)
* [http](./http.md)
* [directors](./directors.md)
---
[^^^ Configuration](./index.md)
