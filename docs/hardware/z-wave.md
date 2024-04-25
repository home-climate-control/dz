Home Climate Control: Hardware Support: Z-Wave
==

## Caveat Emptor

Z-Wave devices are not created equal. Take a close look at their APIs before you buy. Devices of the same class may have drastically different capabilities and behavior.

It would be a good idea to take a look at [Z-Wave JS Config DB Browser](https://devices.zwave-js.io) before buying a device.

## Recommended

* [ZW078 AEON Labs / Heavy Duty Switch](https://devices.zwave-js.io/?jumpTo=0x0086:0x0003:0x004e:0.0)
* [ZW090 AEON Labs / Z‐Stick Gen5 USB Controller](https://devices.zwave-js.io/?jumpTo=0x0086:0x0001:0x005a:0.0) - careful,
different hardware and firmware versions of this device have the same USB manufacturer/device ID (`0658:0200`), but different Z-Wave IDs. 
Devices tested are `0x0086-0x0101-0x005a` (older) and `0x0086-0x0001-0x005a` (newer). Look for the one marked as "works with Raspberry Pi 4" ([here's why](https://community.openhab.org/t/aotect-stick-not-working-on-raspberry-pi-4-solution/106887/3)).
* [ZW096 AEON Labs / Smart Switch 6](https://devices.zwave-js.io/?jumpTo=0x0086:0x0003:0x0060:0.0) - same warning applies. Different hardware versions (some with USB charging ports, some without) have the same Z-Wave ID (`0x0086-0x0103-0x0060`).

## Avoid

* [MP31ZP Minoston / Mini Plug with Power Meter](https://devices.zwave-js.io/?jumpTo=0x0312:0xff00:0xff0e:0.0)

## Testing In progress

* [MP31Z Minoston / Mini Smart Plug](https://devices.zwave-js.io/?jumpTo=0x0312:0xff00:0xff0c:0.0) - interestingly enough, arrived branded as "New One Z-Wave Plug, 800 Series".
---
[^^^ Index](../index.md)
[^^^ Hardware Support](./index.md)
