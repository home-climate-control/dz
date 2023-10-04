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
        changeover-delta: 1
        target-temperature: 22
        keep-hvac-on: true
        controller:
          p: 1
          i: 0.0000008
          limit: 0.7
        mode: cooling
        switch-address: s31zb-00

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

> **NOTE:** this configuration item is the most important for your comfort. A more detailed explanation is coming, for now - step carefully here, especially with the `i` value.

### settings

* `setpoint` defines the default zone setpoint on system start (later overridden by [schedule](./schedule.md))
* `setpoint-range` defines the minimum and maximum allowable setpoint values for this zone.

### economizer

Optional.

> **NOTE:** Economizers are per-zone devices. It is quite possible to have multiple economizers for zones served by the same HVAC unit.

Cooling mode assumed:

* `ambient-sensor`: the sensor used as ambient for this economizer. Zones at different sides of the house may have different ambient temperatures (determined by sun exposure, shade, wind, presence of water, and a zillion of other factors), don't neglect this.
* `changeover-delta`: ambient temperature has to be this much lower than indoors for the economizer to start working (subject to PID configuration).
* `target-temperature`: shut the economizer off when indoor temperature drops to this value.
* `keep-hvac-on`: set to `true` if you want the main HVAC to be still working when the economizer is active (maximum comfort), and to `false` if you want to stop it (maximum cost savings).
* `controller`: just like the zone configuration above.
* `mode`: self-explanatory
* `switch-address`: at this point, the economizer is an on/off device (multistage coming). This is the address of the switch that turns the economizer device on or off.

### Property of
* [home-climate-control](./home-climate-control.md)

### Used in
* [schedule](./schedule.md)
* [http](./http.md)
* [directors](./directors.md)
---
[^^^ Configuration](./index.md)
