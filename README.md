Home Climate Control, a.k.a. DZ
==

Create as many climates in your home as you never thought imaginable.
Control temperature, humidity and ventilation.
Do it on a schedule.
Have everything measured and recorded.
Do it from anywhere.

* **dz3-master** directory contains the DZ Maven project tree;
* **dz3-shell** directory contains a snapshot of a live project, complete with sample configuration.

A few useful links:

* [FAQ: DZ on Raspberry Pi](https://github.com/home-climate-control/dz/wiki/FAQ:-DZ-on-Raspberry-Pi) - the platform of choice
* http://homeclimatecontrol.com/ - project home
* http://diy-zoning.blogspot.com/ - project blog
* http://groups.google.com/group/home-climate-control - user forum
* http://diy-zoning.sourceforge.net/ - legacy project site (yep. This is how it started 20 years ago).

## New Developments

MQTT is now a protocol of choice for remote integrations.

* [HOWTO: MQTT Sensors](https://github.com/home-climate-control/dz/wiki/HOWT:-MQTT-Sensors)
* [DZ as an MQTT Publisher](https://github.com/home-climate-control/dz/wiki/HOWTO:-DZ-as-an-MQTT-Publisher)
* [MQTT Pimoroni Automation Hat Driver](https://github.com/climategadgets/mqtt-automation-hat-go)
* [DZ to Home Assistant Integration](https://github.com/home-climate-control/dz/wiki/HOWTO:-DZ-to-Home-Assistant-integration)

ESP8266/ESP32 is now a future direction for edge device development and integration.
* [hcc-esp8266](https://github.com/home-climate-control/hcc-esp8266) - 1-Wire over MQTT over WiFi on [ESP8266](https://en.wikipedia.org/wiki/ESP8266) ([Arduino IDE](https://github.com/esp8266/Arduino));
* [hcc-esp32](https://github.com/home-climate-control/hcc-esp32) - 1-Wire over MQTT over WiFi on [ESP32](https://en.wikipedia.org/wiki/ESP32) ([ESP-IDF](https://github.com/espressif/esp-idf)).
