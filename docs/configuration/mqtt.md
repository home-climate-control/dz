MQTT connectors
==
There are three at the moment:
* [esphome](./esphome.md)
* [zigbee2mqtt](./zigbee2mqtt.md)
* [zwave2mqtt](./zwave2mqtt.md)

They all share common properties:

```yaml
<connector-type>
  - broker:
      id: <Client ID> # optional, defaults to internally generated stable hash code
      host: <MQTT broker host>
      port: <MQTT broker port> # optional, defaults to 1883
      root-topic: <root topic for all messages for this connector>
      username: <MQTT broker username> # optional, but recommended
      password: <MQTT broker password> # optional, but recommended
      auto-reconnect: <boolean flag> #optional, see below
    sensors:
      ...
    switches:
      ...
    fans:
      ...
```

### broker
MQTT broker configuration.

#### id

Used to identify this client to the MQTT server. Defaults to internally generated consistent identifier based on host and port.

#### auto-reconnect
Instructs the MQTT library to try to reconnect automatically. Generally, it is a good thing, but it may backfire during initial setup because the library does it quietly and will do it forever, with the rest of the system appearing stuck. Use with care.

### sensors, switches, fans

See [sensors, switches, fans](./sensors-switches-fans.md).

### Property of
* [home-climate-control](./home-climate-control.md)

---
[^^^ Configuration](./index.md)
