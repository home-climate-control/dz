Home Climate Control: Hardware Support: Zigbee
==

## Caveat Emptor

Zigbee devices are not created equal. Take a close look at their APIs before you buy. Devices of the same class may have drastically different capabilities and behavior.

It would be a good idea to take a look at [Zigbee2MQTT Supported Devices](https://www.zigbee2mqtt.io/supported-devices/) database before buying a device.

## Prerequisites

* Working [Zigbee2MQTT](https://www.zigbee2mqtt.io/) installation.

## Recommended

* [Sonoff SNZB-02](https://www.zigbee2mqtt.io/devices/SNZB-02_EFEKTA.html) - excellent battery life even when configured for short poll interval.
* [SONOFF ZBDongle-E](https://www.zigbee2mqtt.io/devices/ZBDongle-E.html) - two options are known, both work, though they might report the `linkquality` parameter differently.

## Tested

These are simply known to work, with neither positive nor negative comments.

* [Sengled E1C-NB7](https://www.zigbee2mqtt.io/devices/E1C-NB7.html#sengled-e1c-nb7) - metering outlet. Known to work on high power loads.
* [Sonoff S31ZB](https://www.zigbee2mqtt.io/devices/S31ZB.html#sonoff-s31zb) - non-metering outlet.

## Testing In progress
* [3RSP02028BZ](https://www.zigbee2mqtt.io/devices/3RSP02028BZ.html#third%2520reality-3rsp02028bz) - most detailed reported metrics among metering outlets.  
However, one of six devices started becoming unresponsive and eventually falling off the network and had to be decommissioned. The rest are fine so far.  
Also, the ground prong is hollow, extra caution when connecting high power loads is recommended.
* [ZG-205ZL](https://www.zigbee2mqtt.io/devices/ZG-205ZL.html#tuya-zg-205zl) - human presence detector. Just arrived, no conclusions yet.

---
[^^^ Index](../index.md)
[^^^ Hardware Support](./index.md)
