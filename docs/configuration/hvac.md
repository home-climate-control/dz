hvac
==

Integration with common HVAC devices. Supported kinds of devices are, loosely:

* Heat pumps (a.k.a. "reversible A/C")
* Furnaces (either as a `heatpump` in fixed heating mode, or as a `switchable` device)
* Pure A/C
* Heater Fans
* Radiant Heater
* Oil Heater 
* Motorized Shade
* Economizer

Best explained by example:

```yaml
  hvac:
    - switchable:
        - id: fan-panel-server-room
          mode: cooling
          switch-address: switch-fan-panel-server-room
          filter:
            lifetime: 600H
    - heatpump:
        - id: heatpump-unit1
          switch-mode: null-switch-unit1-mode
          switch-mode-reverse: true
          switch-running: null-switch-unit1-running
          switch-fan: null-switch-unit1-fan
          mode-change-delay: 30S
          filter:
            lifetime: 200H
    - heatpump-hat:
        - id: heatpump-hat
          mode-change-delay: 20S
    - variable:
        - id: hvac-infinity-ac-a6
          mode: cooling
          actuator: fan-infinity-ac-a6
          band-count: 5
          max-power: 0.5

```
### Common Properties

* `id`: Unique identifier this device will be known as to the rest of the system.
* `filter.lifetime:` Optional. Defines the lifetime of the filter this device is equipped with. Will be covered in detail elsewhere; tl:dr: devices keep track of their usage.

### switchable

An on/off HVAC device. Examples: heat fan, radiant heater, oil heater, motorized shade, economizer.

* `mode`: Which mode this device is used in. There can be one. The system will refuse to use this device in the wrong mode. This parameter is mandatory.
* `switch-address`: Address of the switch that turns this device on or off.

### heatpump

* `switch-mode`: Address of the switch that switches this device between heating and cooling mode.
* `switch-mode-reverse`: Set to `true` if the switch must be reversed (this is encountered often). This property exists for other heatpump switches as well.
* `switch-running`: Address of the switch that turns the _condenser_ on or off.
* `switch-fan`: Address of the switch that turns the _air handler_ on or off.
* `mode-change-delay`: Minimum allowed Duration between sending mode change commands. Some HVAC devices dislike frequent changes to the point of shutting themselves off for extended periods of time (up to 30 min) as a safety measure. Consult the HVAC device documentation for details.

### heatpump-hat

This is a Raspberry Pi specific device utilizing a [Pimoroni Automation HAT](https://shop.pimoroni.com/products/automation-hat?variant=30712316554) for controlling a conventional heatpump.

* Relay 0 is used as a mode switch
* Relay 1 is used as a condenser switch
* Relay 2 is used as an air handler switch

No additional configuration is required.

### variable

An on/off HVAC device. Examples: heat fan, radiant heater, oil heater, motorized shade, economizer.

* `mode`: Which mode this device is used in. There can be one. The system will refuse to use this device in the wrong mode. This parameter is mandatory.
* `actuator`: The variable output device used as a HVAC unit. Currently, only [MQTT fans](./mqtt.md#sensors-switches-fans) are supported.
* `max-power`: scale the maximum power output of this device to this much from the total power. The purpose of this parameter is to reduce noise.
* `band-count`: The control signal will be split into this many bands, and the output will not change while the control signal is within the same band. This is done to make the device noise less noticeable, infinitely variable devices annoy people.
  * Default band count is 10, and the maximum possible value is 100. It would probably be a bad idea to raise it above the default.
  * Value of 0 disables banding altogether.

> NOTE: Device power output is first scaled to `max-power`, and then banded by `band-count`. Control quality does not deteriorate with reduced output.

### Property of
* [home-climate-control](./home-climate-control.md)

---
[^^^ Configuration](./index.md)
