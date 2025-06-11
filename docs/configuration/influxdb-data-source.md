Home Climate Control Legacy: DZ as InfluxDB Data Source
==
Psst! Did you read the [BIG FAT WARNING](../index.md#big-fat-warning)?

---
## Prerequisites

[InfluxDB 1.8](https://docs.influxdata.com/influxdb/v1.8/about_the_project/releasenotes-changelog/) - there were reports about InfluxDB 2.0 being incompatible. It's still in beta, let's return to it when it is officially released.

## Integration

InfluxDB publisher configuration is similar to other DZ data sinks (Swing Console, JMX, HttpConnector, [MqttConnector](./mqtt-publisher.md), data loggers) - all you need to do is to add the connector bean to the configuration, and throw in the references to entities you want to publish data streams to InfluxDB. Below is a working example. If you need an authenticated logger, just add username and password as two additional arguments. It is possible to have multiple instances of [InfluxDbLogger](https://github.com/home-climate-control/dz/tree/last-imperative-maintenance/dz3-influxdb/src/main/java/net/sf/dz3/view/influxdb/v1/InfluxDbLogger.java) if you want to send streams to different InfluxDB instances, or if you want to group different entities under different data feed instance names.

```
<bean id="influxdb-sensors-zx" class="net.sf.dz3.view.influxdb.v1.InfluxDbLogger" init-method="start">
  <constructor-arg index="0" type="java.util.Set">
    <set>
      <ref bean="mqtt_sensor-2A0300A27980C028"/>
      <ref bean="temperature_sensor-0013A200.4062AC9A_A0"/>
      <ref bean="temperature_sensor-0013A200.4062AC98_A0"/>
      <ref bean="temperature_sensor-0013A200.405D8027_A0"/>
      <ref bean="temperature_sensor-500000027CD07C28"/>
      <ref bean="temperature_sensor-9100000044C91528"/>
      <ref bean="temperature_sensor-960000027CC03628"/>
      <ref bean="temperature_sensor-9C0000027CAEEA28"/>
      <ref bean="temperature_sensor-C700000044867B28"/>
      <ref bean="temperature_sensor-vcgencmd-pi1-0"/>
      <ref bean="temperature_sensor-vcgencmd-pi1-1"/>
      <ref bean="temperature_sensor-vcgencmd-pi2-0"/>
      <ref bean="temperature_sensor-vcgencmd-pi2-1"/>
      <ref bean="mqtt_sensor-380300A279AABA28"/>
    </set>
  </constructor-arg>
  <!-- Instance name -->
  <constructor-arg index="1" type="java.lang.String" value="house"/>
  <!-- InfluxDB URL to connect to -->
  <constructor-arg index="2" type="java.lang.String" value="http://influxdb.house.local:8086/"/>
</bean>
```

---
PS: Compare to [how the InfluxDB integration is configured today](https://github.com/home-climate-control/dz/blob/master/docs/configuration/influx.md)
