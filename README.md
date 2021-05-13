Home Climate Control, a.k.a. DZ
==

Create as many climates in your home as you never thought imaginable.  
Control temperature, humidity and ventilation.  
Do it on a schedule.  
Have everything measured and recorded.  
Do it from anywhere.

# Pardon Our Dust...

A brief glance at the [version control history](https://github.com/home-climate-control/dz/network) will tell you that something major has just happened. Yes, a major change is underway. In short, it is now possible to build the project in one take. Due to the separation between the code itself, and the [project Wiki](https://github.com/home-climate-control/dz/wiki), it will take a bit to make it all consistent. If in doubt, don't hesitate to post a message to our [user forum](http://groups.google.com/group/home-climate-control), help will come fast.

Meanwhile, this should get you going:

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
