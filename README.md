Home Climate Control, a.k.a. DZ
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

# Major Update: XML to YAML Configuration Switchover

Effective September 10 2023, XML based configuration is gone from everywhere except the [imperative branch](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance). The [reactive](https://github.com/home-climate-control/dz/tree/reactive) tree, soon to become the trunk, is now using YAML configuration. Detailed reference is located [here](./docs/configuration/index.md).

Summary of changes from imperative code:
* Configuration is now code independent.
* Configuration is now [extensively documented](./docs/configuration/index.md).
* Configuration supports YAML anchors.
* Configuration code assist is now available when you're editing it in an IDE.
* [Mock switches](./docs/configuration/mocks.md) are now available out of the box.
* Zones now support min/max allowed range, per zone.
* New device: [economizer](./docs/configuration/zones.md#economizer) is now available, per zone.
* New devices: [switchable](./docs/configuration/hvac.md#switchable) and [heatpump-hat](./docs/configuration/hvac.md#heatpump-hat) are now available along with the [heatpump](./docs/configuration/hvac.md#heatpump).
* New access mechanism: [web-ui](./docs/configuration/web-ui.md) is now being developed as a first class interface.
* New functionality: [console](./docs/configuration/console.md) now supports the [economizer](./docs/configuration/zones.md#economizer), [instrument cluster](./docs/instrument-cluster/index.md) ("system status at a glance"), and individual entries for additional sensors.

## Psst!

* Finally, [a shot at decent documentation](./docs/index.md). Work in progress, changes often. Anything missing? [Please let us know](http://groups.google.com/group/home-climate-control).
* There's a fresh new driver for the [Pimoroni Automation HAT](https://learn.pimoroni.com/tutorial/sandyj/getting-started-with-automation-hat-and-phat) that has just been integrated with DZ, here: https://github.com/home-climate-control/automation-hat-driver. This is the most compact HVAC relay solution so far.
* Dallas Semiconductor 1-Wire API that was integrated into DZ codebase some 20 years ago, survived, was hardened and optimized, is now a separate project again, here: [https://github.com/home-climate-control/owapi-reborn/](https://github.com/home-climate-control/owapi-reborn/)
* XBee radio driver has just flipped from imperative to reactive: [https://github.com/home-climate-control/xbee-api-reactive](https://github.com/home-climate-control/xbee-api-reactive)

# Pardon Our Dust...

A brief glance at the [version control history](https://github.com/home-climate-control/dz/network) will tell you that something major is happening. Yes, a major change is underway - [migration to Reactive Streams](https://github.com/home-climate-control/dz/milestone/12). This is almost a complete overhaul, and is certainly a backward incompatible change.

## What is What, Where, and When
* The long in the tooth, but rock stable code base now resides at [last-imperative-maintenance](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance) branch.
* The bleeding edge development is happening at [reactive](https://github.com/home-climate-control/dz/tree/reactive) branch. This code base has already been deployed to one production installation and is about to be deployed to the second.
* Once things are stabilized, `reactive` will be merged to `dev` and then to `master`.

### If you're an existing user...
...you might want to wait a bit (until this message is gone).

### If you just found this...
...then going for the [reactive](https://github.com/home-climate-control/dz/tree/reactive) code base is your best shot. It is still raw, but is being actively worked on. And the learning curve is much easier.

## Documentation
 [project Wiki](https://github.com/home-climate-control/dz/wiki), is a good source for general information. Up to date branch specific information is available from the [project documentation root](./docs/index.md). If in doubt, don't hesitate to post a message to our [user forum](http://groups.google.com/group/home-climate-control), help will come fast.

## Quick Start
This should get you going:

```
git clone https://github.com/home-climate-control/dz.git && \
cd dz && \
git submodule init && \
git submodule update && \
./gradlew build installDist
```

When completed successfully, it will create an executable script in `${project_root}./dz3-spring/build/install/dz/bin/dz`. The next step would be to [create the configuration](https://github.com/home-climate-control/dz/wiki/Configuration) and run the script with its path as an argument.

## And now back to our regularly scheduled programming...

A few useful links:

* [FAQ: DZ on Raspberry Pi](https://github.com/home-climate-control/dz/wiki/FAQ:-DZ-on-Raspberry-Pi) - the platform of choice
* http://homeclimatecontrol.com/ - project home
* http://diy-zoning.blogspot.com/ - project blog
* http://groups.google.com/group/home-climate-control - user forum
* http://diy-zoning.sourceforge.net/ - legacy project site (yep. This is how it started 20 years ago).

## New Developments

MQTT is now a protocol of choice for remote integrations.

* [MQTT Sensors](https://github.com/home-climate-control/dz/wiki/HOWTO:-MQTT-Sensors)
* [DZ as an MQTT Publisher](https://github.com/home-climate-control/dz/wiki/HOWTO:-DZ-as-an-MQTT-Publisher)
* [MQTT Pimoroni Automation Hat Driver](https://github.com/climategadgets/mqtt-automation-hat-go)
* [DZ to Home Assistant Integration](https://github.com/home-climate-control/dz/wiki/HOWTO:-DZ-to-Home-Assistant-integration)
* [DZ to ESPHome Integration](https://github.com/home-climate-control/dz/wiki/HOWTO:-DZ-to-ESPHome-integration)

ESP8266/ESP32 is now a future direction for edge device development and integration.

* ~~[hcc-esp8266](https://github.com/home-climate-control/hcc-esp8266) - 1-Wire over MQTT over WiFi on [ESP8266](https://en.wikipedia.org/wiki/ESP8266) ([Arduino IDE](https://github.com/esp8266/Arduino))~~ Use [ESPHome Integration](https://github.com/home-climate-control/dz/wiki/HOWTO:-DZ-to-ESPHome-integration) instead
* [hcc-esp32](https://github.com/home-climate-control/hcc-esp32) - 1-Wire over MQTT over WiFi on [ESP32](https://en.wikipedia.org/wiki/ESP32) ([ESP-IDF](https://github.com/espressif/esp-idf))

InfluxDB is now supported as a data sink.

* [DZ as InfluxDB Data Source](https://github.com/home-climate-control/dz/wiki/HOWTO:-DZ-as-InfluxDB-Data-Source)
