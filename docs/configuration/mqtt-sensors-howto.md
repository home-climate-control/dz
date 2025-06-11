Home Climate Control Legacy: HOWTO: MQTT Sensors
==

Psst! Did you read the [BIG FAT WARNING](../index.md#big-fat-warning)?

## Work In Progress
See [#334](https://github.com/home-climate-control/dz/issues/334).

---
## Configuration

Configuring MQTT sensors is similar to other factory based configurations 
([1-Wire](https://github.com/home-climate-control/dz/wiki/HOWTO:-1-Wire-Sensors) 
and [XBee](https://github.com/home-climate-control/dz/wiki/HOWTO:-XBee-Sensors)). Here's a working example:

```
<!-- MQTT Device Factory. You can have more than one if you have several brokers. -->
<bean id="mqtt-device-factory"
    class="net.sf.dz3.view.mqtt.v1.MqttDeviceFactory"
    destroy-method="powerOff">
    <!-- Broker host -->
    <constructor-arg index="0" value="mqt.broker.host" />
    <!-- Root topic - publish -->
    <constructor-arg index="1" value="/hcc/instance" />
    <!-- Root topic - subscribe -->
    <constructor-arg index="2" value="/hcc/sensor" />
</bean>

<bean id="mqtt_sensor-289E0279A201039B" factory-bean="mqtt-device-factory"
    factory-method="getSensor">
    <constructor-arg value="289E0279A201039B" />
</bean>
```

Below is the JSON that DZ expects. mandatory fields are `entity_type=sensor`, `name`, and
`signal`. DZ doesn't care about the topic as long as it is under
`${root-topic-subscribe}/` - it parses JSON for data.

```
{
    "device_id": "ESP8266-00621CC5",
    "entity_type": "sensor",
    "name": "289E0279A201039B",
    "signal": 23.87,
    "signature": "T289E0279A201039B"
}
```

See also: [MQTT: Useful Tools, Bits and Pieces](./mqtt-bits-and-pieces.md)

---
PS: Compare to [how MQTT Connectors are configured today](https://github.com/home-climate-control/dz/blob/master/docs/configuration/mqtt.md).
