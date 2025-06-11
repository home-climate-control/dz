Home Climate Control Legacy: HOWTO: 1-Wire Sensors
==

Psst! Did you read the [BIG FAT WARNING](../index.md#big-fat-warning)?

## Work In Progress
See [#334](https://github.com/home-climate-control/dz/issues/334).

---
Configuring 1-Wire sensors is similar to other factory based configurations ([MQTT](mqtt-sensors-howto.md) and [XBee](./xbee-sensors-howto.md)).

## Serial 1-Wire Adapter

Here's a working example:

```
<!-- 1-Wire Device Factory -->
<bean id="onewire_device_factory"
    class="net.sf.dz3.device.sensor.impl.onewire.OwapiDeviceFactory"
    init-method="start">
    <constructor-arg index="0" value="/dev/ttyUSB0" />
    <constructor-arg index="1" value="regular" />
</bean>

<bean id="temperature_sensor-C700000044867B28" factory-bean="onewire_device_factory"
    factory-method="getTemperatureSensor">
    <constructor-arg value="C700000044867B28" />
</bean>
```

## Raspberry Pi Native

Coming up. Stay tuned.

___
PS: Compare to [how 1-Wire Sensors are configured today](https://github.com/home-climate-control/dz/blob/master/docs/configuration/1-wire.md).
