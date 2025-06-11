Home Climate Control Legacy: HOWTO: XBee Sensors
==
Psst! Did you read the [BIG FAT WARNING](../index.md#big-fat-warning)?

---
Configuring XBee sensors is similar to other factory based configurations ([1-Wire](./1-wire-sensors-howto.md) and [MQTT](mqtt-sensors-howto.md)), though it's a bit more tricky. Here's a working example, the XBee has a [TMP36](https://www.analog.com/en/products/tmp36.html) analog sensor (another option is [LM34](http://www.ti.com/product/LM34)):

```
<!-- XBee Device Factory -->
<bean id="xbee_device_factory" class="net.sf.dz3.device.sensor.impl.xbee.XBeeDeviceFactory"
    init-method="start">
    <constructor-arg index="0" value="/dev/ttyUSB1" />
</bean>

<!-- Raw ADC sensor, channel A0 -->
<bean id="voltage_sensor-0013A200.4062AC98_A0"
    factory-bean="xbee_device_factory"
    factory-method="getTemperatureSensor">
    <constructor-arg value="0013A200.4062AC98:A0"/>
</bean>

<!-- This is what we need to convert the raw voltage into temperature -->
<bean id="converter_TMP36" class="net.sf.dz3.device.sensor.impl.AnalogConverterTMP36" />

<!-- Or this, if you're using LM34 -->
<bean id="converter_LM34" class="net.sf.dz3.device.sensor.impl.AnalogConverterLM34" />

<bean id="temperature_sensor-0013A200.4062AC98_A0"
    class="net.sf.dz3.device.sensor.impl.ConvertingSensor">
    <constructor-arg index="0" ref="voltage_sensor-0013A200.4062AC98_A0" />
    <constructor-arg index="1" ref="converter_TMP36" />
    <!-- Calibration shift - analog sensors are finicky -->
    <constructor-arg index="2" value="1.7" />
</bean>
```
## Fair Warning

At the time DZ first started considering wireless solutions, XBee was a sensible option - very little else existed.
Today, XBee seems to be terribly overpriced for the value it delivers, and too complicated to get going.
You probably want to consider more contemporary wireless alternatives (see [MQTT Sensors](mqtt-sensors-howto.md)
and [ESPHome based](https://github.com/home-climate-control/dz/blob/master/docs/configuration/esphome.md),
[current code base](https://github.com/home-climate-control/dz?tab=readme-ov-file#home-climate-control) only).

If the above paragraph didn't discourage you, feel free to proceed to [DZ XBee HOWTO](https://diy-zoning.blogspot.com/2010/07/dz-xbee-howto.html) - but you're on your own. If it breaks, you get to keep both pieces. You've been warned.

## Having said that...

Getting new and shiny ESP* devices, with their capabilities far superior to XBee, feels good, however - relying on them, at least in their WiFi incarnation, introduces a single point of failure to your whole installation.

So, if you're considering hardening your installation and XBee is an option you're willing to entertain, [throw in your vote on DZ forum](https://groups.google.com/group/home-climate-control) - that's a good chance to have this document made up-to-date and usable.
