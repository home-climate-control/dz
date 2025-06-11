Home Climate Control Legacy: DZ as an MQTT Publisher
==
Psst! Did you read the [BIG FAT WARNING](../index.md#big-fat-warning)?

---
**Before you start:** [MQTT: Useful Tools, Bits and Pieces](./mqtt-bits-and-pieces.md)

MQTT configuration is similar to other DZ data sinks (Swing Console, JMX, HttpConnector, [InfluxDB](./influxdb-data-source.md), data loggers) - all you need to do is to add the connector bean to the configuration, and throw in the references to entities you want to publish status of via MQTT. Here's a working example:

```
<!-- MQTT Connector -->
<bean id="mqtt-connector"
      class="net.sf.dz3.view.mqtt.v1.MqttConnector"
      init-method="activate">
    <!-- Broker host -->
    <constructor-arg index="0" value="${your_mqtt_broker_host}" />
    <!-- Root topic - publish -->
    <constructor-arg index="1" value="${your_dz_instance_name}" />
    <!-- Root topic - subscribe -->
    <constructor-arg index="2" value="${subscribe_topic}" />
    <!-- Reported entities -->
    <constructor-arg index="3" type="java.util.Set">
        <set>
            <!-- Throw anything you want in here, if it is not supported,
                 it will log a warning. And you want it supported,
                 send a message to the forum, it's likely trivial to add -->
                <ref bean="temperature_sensor-temp1" />
                <ref bean="temperature_sensor-temp2" />
                <ref bean="thermostat-temp1" />
                <ref bean="thermostat-temp2" />
                <ref bean="zone_controller-cpu" />
                <ref bean="hvac_controller-server-room" />
            </set>
        </constructor-arg>
    </bean> 
```

This would publish the status of all included entities into MQTT topics rooted at `${your_dz_instance_name}`.

It would be a good idea to form `${your_dz_instance_name}` in a hierarchical way to allow topic filtering, and integrating multiple DZ instances with the same MQTT broker. `/hcc/$instance/` works just fine.

`${subscribe_topic}` has no special restrictions (other that it can't be the same as `${your_dz_instance_name}` - DZ will fast fail if it is), but it would also be a good idea to follow the same naming conventions.

## What's next?

You might want to explore [DZ to Home Assistant integration](./home-assistant-integration.md), and [HOWTO: MQTT Sensors](./mqtt-sensors-howto.md).

---
PS: Compare to [how MQTT Connectors are configured today](https://github.com/home-climate-control/dz/blob/master/docs/configuration/mqtt.md).
