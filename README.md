Home Climate Control
==

[![Build Status](https://app.travis-ci.com/home-climate-control/dz.svg)](https://app.travis-ci.com/github/home-climate-control/dz)
[![Build Status](https://github.com/home-climate-control/dz/actions/workflows/gradle.yml/badge.svg)](https://github.com/home-climate-control/dz/actions/workflows/gradle.yml)
[![Build Status](https://github.com/home-climate-control/dz/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/home-climate-control/dz/actions/workflows/codeql-analysis.yml)
[![SonarCloud](https://github.com/home-climate-control/dz/actions/workflows/sonarcloud.yml/badge.svg)](https://github.com/home-climate-control/dz/actions/workflows/sonarcloud.yml)

*Create as many climates in your home as you never thought imaginable.*  
*Control temperature, humidity and ventilation.*  
*Do it on a schedule.*  
*Have everything measured and recorded.*  
*Do it from anywhere.*

# Pardon Our Dust...

A [major overhaul](https://github.com/home-climate-control/dz/milestone/12) that's been happening since October 2021 is [almost] complete.
Here's a summary of user visible changes against the [legacy code](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance):

* Configuration is now YAML based, code independent, and [extensively documented](./docs/configuration/index.md).
* Configuration code assist is now available when you're editing it in an IDE.
* [ESPHome](https://esphome.io), Zigbee and Z-Wave devices are [now supported via MQTT](./docs/configuration/mqtt.md).
* [Mock switches](./docs/configuration/mocks.md) are now available out of the box.
* Zones now support [min/max allowed range](./docs/configuration/zones.md#settings), per zone.
* New device: [economizer](./docs/configuration/zones.md#economizer) is now available, per zone.
* New devices: [switchable](./docs/configuration/hvac.md#switchable) and [heatpump-hat](./docs/configuration/hvac.md#heatpump-hat) are now available along with the [heatpump](./docs/configuration/hvac.md#heatpump).
* New access mechanism: [web-ui](./docs/configuration/web-ui.md) is now being developed as a first class interface.
* New functionality: [console](./docs/configuration/console.md) now supports the [economizer](./docs/configuration/zones.md#economizer), [instrument cluster](./docs/instrument-cluster/index.md) ("system status at a glance"), and individual entries for additional sensors.
* Observability: [InfluxDB integration](./docs/configuration/influx.md) emits massive amounts of usable information about your house with Micrometer's help.
* Last, but not least, can now be run in a [Docker container](./docs/build/index.md#docker).

## Stragglers
XBee and 1-Wire device support could not have been converted due to limited hardware available for testing.
Starting now, the overhauled code is being dogfooded at main production deployment location, so expect progress soon.

## But doesn't this bleeding edge bleed too much?
Possibly. If that bothers you, the long in the tooth, but rock stable code base now resides at [last-imperative-maintenance](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance) branch.

## Documentation
Your best bet is to read the documentation that resides in the branch you're looking at, at [./docs/index.md](./docs/index.md)
If in doubt, don't hesitate to post a message to our [user forum](http://groups.google.com/group/home-climate-control), help will come fast.

## Next Steps
* [FAQ](./docs/index.md#faq)
* [Build](./docs/build/index.md)
* [Configuration Reference](./docs/configuration/index.md)
