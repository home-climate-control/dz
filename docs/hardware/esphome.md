Home Climate Control: Hardware Support: ESPHome Compatible
==

All the following are being extensively used in varying configurations; pick the one that suits you.
Still, consider [Zigbee](./zigbee.md) first, they are better for basic use.

## Boards

* [Adafruit HUZZAH](https://www.adafruit.com/product/2821) board
* [Adafruit HUZZAH32](https://www.adafruit.com/product/3405) board
* [Wemos D1](https://www.wemos.cc/en/latest/d1/index.html) compatible boards - current choice unless more processing power is required
**WARNING:** Be careful using them with [1-Wire](./1-wire.md) devices, [here's why](https://github.com/esphome/issues/issues/3415) (not fatal, but very annoying and reproducible).

## Sensors

* See [1-Wire](./1-wire.md), [Dallas component](https://esphome.io/components/sensor/dallas.html)
* See [I2C](./i2c.md), [I²C Bus](https://esphome.io/components/i2c#i2c)

## Actuators

* [Lilygo T-Relay](https://github.com/Xinyuan-LilyGO/LilyGo-T-Relay) - don't forget to get their T-U2T dongle to program this board. This board can support additional devices - using [I²C](./i2c.md) sensors here would be a good idea.

---
[^^^ Index](../index.md)
[^^^ Hardware Support](./index.md)
