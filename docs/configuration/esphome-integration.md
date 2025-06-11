Home Climate Control Legacy: HOWTO: ESPHome Integration
==

Psst! Did you read the [BIG FAT WARNING](../index.md#big-fat-warning)?

## Work In Progress
See [#334](https://github.com/home-climate-control/dz/issues/334).

---

## Prerequisites

- Working [ESPHome](https://esphome.io/) installation;
- Probably at least some sensors:
    - https://www.esphome.io/components/sensor/;
    - https://www.esphome.io/components/sensor/dallas.html - 1-Wire is still the best bang for the buck.

## The Home Assistant Way

ESPHome is well integrated with [Home Assistant](https://www.home-assistant.io/). If you do already use Home Assistant, you might find [HOWTO: DZ to Home Assistant integration](./home-assistant-integration.md) the most convenient route (thought it's not the lightest).

## The ESPHome way (simplest, currently preferred)

### Step 1/2. Configure ESPHome [MQTT Client Component](https://www.esphome.io/components/mqtt.html) on your sensors.

Heed the warnings in [Notable Details](#notable-details) section, and take a look at [MQTT: Useful Tools, Bits and Pieces](./mqtt-bits-and-pieces.md).

### Step 2/2. Configure ESPHome sensors in DZ configuration.

Here's a working example:

```
  <!-- IMPERATIVE -->
  <!-- ESPHome MQTT Device Factory -->
  <bean id="esphome-device-factory"
        class="net.sf.dz3.view.mqtt.v1.ESPHomeDeviceFactory"
        destroy-method="powerOff">
    <!-- Broker host -->
    <constructor-arg index="0" value="dz-house"/>
    <!-- Root topic - publish -->
    <constructor-arg index="1" value="/hcc/house"/>
    <!-- Root topic - subscribe -->
    <constructor-arg index="2" value="/esphome"/>
  </bean>
  <!-- ESPHome sensors -->
  <bean id="esphome_sensor-kitchen-1wire-0"
        factory-bean="esphome-device-factory"
        factory-method="getSensor">
    <constructor-arg value="kitchen-1wire-0"/>
  </bean>
```
You're done, `esphome_sensor-kitchen-1wire-0` is the name of the sensor you can use from this point on.

## The Long Way (useful elsewhere)

This integration was implemented for generic MQTT producers before ESPHome was discovered. `ESPHomeDeviceFactory` made it obsolete for ESPHome integration, but it is still valid for generic MQTT devices, so it is retained here as an example.

### Step 1/3. Configure ESPHome [MQTT Client Component](https://www.esphome.io/components/mqtt.html) on your sensors.
### Step 2/3. Configure sensors to send JSON payload over MQTT.

Here's a working example:

```
dallas:
  - pin: GPIO4
    update_interval: 5s

sensor:
  - platform: dallas
    address: 0xBC0000027CA43928
    id: BC0000027CA43928
    resolution: 12
    accuracy_decimals: 4
    name: "test drive A"
    retain: false
    on_value:
        then:
            - mqtt.publish_json:
                topic: /hcc/sensor/BC0000027CA43928
                payload:
                    root["entity_type"] = "sensor";
                    root["name"] = "BC0000027CA43928";
                    root["signature"] = "TBC0000027CA43928";
                    root["signal"] = id(BC0000027CA43928).state;
                    root["device_id"] = "ESP8266-00E470CD";

mqtt:
  broker: [ip-address]
  topic_prefix: /esphome
  log_topic: /esphome-log/00E470CD
```
#### Notable Details
- JSON entries DZ absolutely requires to recognize the payload as coming from a [DZ Edge Device](https://github.com/home-climate-control/dz/issues/113) are:
    - entity_type = sensor
    - name
    - signal
- The rest are optional, but DZ will be able to make more sense out of the payload if they are present.
- The name that gets passed to DZ is `root["name"]`, and not YAML `sensor/name` - that one goes to Home Assistant.
- You want `retain: false` - ESPHome doesn't emit timestamps, and retained MQTT sensor readings are likely to be incorrect and throw consumers off (default is `retain: true`).
- You definitely want `accuracy_decimals: 4`, the default value of 1 will degrade DZ control quality.
- You definitely want `update_interval` to be more often than the default 60 seconds. 30 is fine, 10 is better, 5 is awesome.
- You don't need to worry about `expire_after`, DZ keeps track of sensor readings itself.
- See if you want any [sensor filters](https://www.esphome.io/components/sensor/index.html#sensor-filters) configured - you might, if you're using analog sensors, or need to calibrate your sensors.
- DZ doesn't care about the topic much; all you need to do is to make sure that `topic` provided us a subtopic of the topic DZ listens on.

### Step 3/3. Configure DZ to consume MQTT sensors.

Follow directions in [HOWTO: MQTT Sensors](./mqtt-sensors-howto.md).
