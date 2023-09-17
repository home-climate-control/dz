sensors & switches
==

### sensors

Best explained by example:
```yaml
sensors:
  - address: family-room-temperature
  - id: theater-room-temperature
    address: theater-room-temperature-bme280
    timeout: 45S
```

The first entry uses the sensor configured with `family-room-temperature` name, with no translation.
The second entry translates the hardware dependent sensor name into `theater-room-temperature` name this sensor will be known to HCC as.

The first way will work just fine with [ESPHome](https://esphome.io/) and [zigbee2mqtt](https://www.zigbee2mqtt.io/).
The second way will be generally required with [zwave2mqtt](https://github.com/zwave-js/zwave-js-ui) as their topic conventions are more complicated.

#### timeout
This parameter defines maximum allowable interval between measurements. If the signal doesn't come, timeout signal will be issued, and repeated every timeout interval. Default timeouts are:

* For [ESPHome](https://esphome.io/) based devices: 30 seconds.
* For [Z-Wave JS UI](https://github.com/zwave-js/zwave-js-ui#z-wave-js-ui) (formerly [ZWave To MQTT](https://github.com/OpenZWave/Zwave2Mqtt#zwave-to-mqtt)): 90 seconds
* For [Zigbee2MQTT](https://www.zigbee2mqtt.io/): 90 seconds.

Generally, you want to have sensors to emit measurements every five to ten seconds. 
Zigbee and Z-Wave devices default to much higher periods and need to be specifically configured to emit faster. 
This affects battery life, however, at least for the HCC workhorse, [SNZB-02](https://www.zigbee2mqtt.io/devices/SNZB-02.html), setting the reporting interval to 10 seconds still yields many months of battery life.

### sensors - unit of measurement

Some sources (like `esphome`) allow or require sensor names to be unique, so, for example, there will be two sensors: `family-room-temperature` and `family-room-humidity`.

Some others (notably `zigbee2mqtt` and probably `zwave2mqtt`, tbd) report multiple measurements at the same sensor name. This requires special syntax:

```yaml
zigbee2mqtt:
  - host: mqtt-zigbee
    root-topic: zigbee2mqtt
    sensors:
      - id: ambient-patio-temperature
        address: SNZB-02-01-patio
        measurement: temperature
      - id: ambient-patio-humidity
        address: SNZB-02-01-patio
        measurement: humidity
```

If no `measurement` is provided in the configuration, the default measurement of `temperature` is provided.

> **NOTE:** `zigbee2mqtt` devices will generally return more measurements. Case in point, [SNZB-02](https://www.zigbee2mqtt.io/devices/SNZB-02.html) returns `battery`, `temperature`, `humidity`, `voltage`, and `linkquality`. Only two of them are relevant to climate control, but others are vital for system health and will also be available. This will be covered elsewhere.

### switches
Same as above, but with more attributes:
```yaml
switches:
  - id: zw096-blue
    address: zwave/Workshop/ZW096-blue
    reversed: <boolean flag>
    heartbeat: <Duration>
    pace: <Duration>
```

#### reversed
Set to true if the hardware switch state must be set to the opposite of the logical.

#### heartbeat
Send the command to hardware this often even if the logical state hasn't changed. Good for fault tolerance (yes, people yank power cords out by accident sometimes).

#### pace
Send the same command to hardware no more often that this. Some bridges (notably `zigbee2mqtt`) are known to become unresponsive with no error indication when incoming traffic exceeds their bandwidth.

### Property of
* [esphome](./esphome.md)
* [zigbee2mqtt](./zigbee2mqtt.md)
* [zwave2mqtt](./zwave2mqtt.md)
* [mocks](./mocks.md)

---
[^^^ Configuration](./index.md)  
[^^^MQTT Connectors](./mqtt.md)
