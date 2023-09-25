MQTT connectors
==
There are three at the moment:
* [esphome](./esphome.md)
* [zigbee2mqtt](./zigbee2mqtt.md)
* [zwave2mqtt](./zwave2mqtt.md)

They all share common properties:

```yaml
<connector-type>
  - host: <MQTT broker host>
    port: <MQTT broker port> # optional, defaults to 1883
    root-topic: <root topic for all messages for this connector>
    username: <MQTT broker username> # optional, but recommended
    password: <MQTT broker password> # optional, but recommended
    auto-reconnect: <boolean flag> #optional, see below
    sensors:
      ...
    switches:
      ...
```

### auto-reconnect
Instructs the MQTT library to try to reconnect automatically. Generally, it is a good thing, but it may backfire during initial setup because the library does it quietly and will do it forever, with the rest of the system appearing stuck. Use with care.

### sensors & switches

See [sensors & switches](./sensors-and-switches.md).

### Property of
* [home-climate-control](./home-climate-control.md)

---
[^^^ Configuration](./index.md)
