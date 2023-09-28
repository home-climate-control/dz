home-assistant
==

Integration that allows [Home Assistant](https://www.home-assistant.io/) to automatically discover HCC zones as its [Climate](https://www.home-assistant.io/integrations/climate/) integrations, view their status and control them.

Best explained by example:

Minimal configuration:

```yaml
  connectors:
    - home-assistant:
        broker:
          host: mqtt-hass
```

Full configuration:

```yaml
  connectors:
    - home-assistant:
        broker:
          host: mqtt-hass
          root-topic: <see below>
        discovery-prefix: <Home Assistant MQTT Discovery topic> # optional, defaults to "homeassistant"
        node-id: <This instance's ID> # optional, defaults to `home-climate-control.instance` value
        zones:
          - Workshop-east
          - Workshop-west
```

### broker
Configuration is common with other [MQTT integrations](./mqtt.md), with the exception of the `root-topic`. Here, it must be left empty, and `discovery-prefix` provided instead.

### discovery-prefix
MQTT topic configured in Home Assistant for MQTT discovery. You probably don't need to provide it.

### node-id
Home Assistant [doesn't care about it](https://www.home-assistant.io/integrations/mqtt#mqtt-discovery), but you might, to keep your MQTT topic tree tidy.  You probably don't need to provide it, default will likely work well.

> **NOTE:** Due to [Quarkus](../build/index.md#quarkus) quirks, `root-topic` must be specified even if it is at Home Assistant default, but `discovery-prefix` must be omitted. Don't ask.

Please see [Home Assistant MQTT Discovery](https://www.home-assistant.io/integrations/mqtt#mqtt-discovery) specification for further information. Keep in mind that most elements are provided by HCC automatically.
Should there be difficulties, [MQTT Explorer](https://mqtt-explorer.com/) can provide priceless insights.

### zones
List of [zone.id](./zones.md#id)s to expose and accept commands for. Optional, if not present, all zones at all [directors](./directors.md) are exposed.

### Principal limitation of Home Assistant climate control

HCC has from the very beginning been designed as a [multizone system](https://www.homeclimatecontrol.com/faq/temperature-zoning-and-climate-control#h.p_tjs44rqXagyY).
Surprisingly, over 20 years after its inception, this concept didn't get acknowledged by mainstream HVAC related projects, and even by many HVAC hardware manufacturers.

HCC does not, and will not, expose mode switching (heat/cool) to any system that does not specifically understand the concept of "multizone". Here's why:

- Multizone systems (such as HCC) realize that several zones may be controlled by the same HVAC unit, whereas non-multizone aware systems (such as Home Assistant) do not;
- It would be awkward to allow HVAC units to be turned on or off with no regard to how other zones are commanding them at the moment;
- It would be somewhere between awkward and catastrophic to issue a mode change command to just one zone, or, even worse, issued different mode commands to different zones served by the same HVAC unit.

Until this restriction is lifted, the only things Home Assistant (and others) will be allowed to control would be just single thermostat properties. The OFF mapping in this case will disable an individual [zone](./zones.md).
