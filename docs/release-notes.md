Home Climate Control: Release Notes
==

## v4.4.0 (2024-08-15)

* Enhancement: [#322](https://github.com/home-climate-control/dz/issues/322) - eliminated control input lag.

## v4.3.0 (2024-06-04)

* Enhancement: [#267](https://github.com/home-climate-control/dz/issues/267) - economizer can now exist without a setpoint.
* Bugfix: [#320 Economizer runs forever when there is an ambient sensor failure](https://github.com/home-climate-control/dz/issues/320)
* Breaking change: [economizer configuration syntax](../docs/configuration/zones.md#economizer) changed to allow future enhancements

## v4.2.0 (2024-01-17)

* Project layout decluttered - now it is organized into [./app](../app), [./modules](../modules), and [./submodules](../submodules) instead of a big flat mess ([#304](https://github.com/home-climate-control/dz/issues/304)).
* Stability improvement - a single invalid Zigbee2MQTT no longer causes a cascading failure ([#303](https://github.com/home-climate-control/dz/issues/303))
* Important comfort improvement ([#300](https://github.com/home-climate-control/dz/issues/300))

## v4.1.0 (2023-11-28)
Major milestones:

* Any HVAC device can now be used as an economizer ([#291](https://github.com/home-climate-control/dz/issues/291))
* Major fault tolerance improvement, better telemetry and diagnostics ([#292](https://github.com/home-climate-control/dz/issues/292))
* Variable output single mode HVAC devices are now supported ([#293](https://github.com/home-climate-control/dz/issues/293))

[read more...](./release-notes/v4.1.0.md)

## v4.0.0 (2023-10-12)
Major milestones:
* [YAML configuration](../docs/configuration/index.md)
* Extended [list of supported devices](../docs/hardware/index.md)
* Now [runs in a container](../docs/build/index.md#docker)
* Now [integrated with Home Assistant](../docs/configuration/home-assistant.md)

[read more...](./release-notes/v4.0.0.md)

## pre-v4.0.0

Strict release policy wasn't followed, see [tags](https://github.com/home-climate-control/dz/tags) for stable revisions.

---
[^^^ Index](./index.md)
