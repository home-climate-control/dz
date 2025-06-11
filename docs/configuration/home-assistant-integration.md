Home Climate Control Legacy: Home Assistant Integration
==
Psst! Did you read the [BIG FAT WARNING](../index.md#big-fat-warning)?

---
## Prerequisites
* [DZ is configured as an MQTT Publisher](./mqtt-publisher.md);
* [MQTT](https://www.home-assistant.io/integrations/mqtt/) is enabled in Home Assistant (HASS hereinafter);
* If you just want HASS to recognize DZ sensors:
    * [MQTT Sensor](https://www.home-assistant.io/integrations/sensor.mqtt/) is enabled (see below);
* If you want to expose DZ's thermostats to HASS and allow HASS to control them:
    * [MQTT Eventstream](https://www.home-assistant.io/integrations/mqtt_eventstream) is enabled in HASS (`publish_topic` is mandatory, `subscribe_topic` is irrelevant) (see below);
    * [MQTT HVAC](https://www.home-assistant.io/integrations/climate.mqtt) is enabled in HASS (see below).

## Making Home Assistant recognize DZ sensors

Add this block to `configuration.yaml`:
```
sensor:
  - platform: mqtt
    name: "Ambient"
    state_topic: "${your_dz_instance_name}/sensor/${dz_sensor_name}"
    value_template: "{{ value_json.signal}}"
    unit_of_measurement: "°C"
```
- `${your_dz_instance_name}` is the same name you provided when you [configured DZ as an MQTT publisher](./mqtt-publisher.md)
- `${dz_sensor_name}` is the same value you provided as a sensor name in DZ configuration. You can confirm correct values if you point your MQTT client at all topics starting with `${your_dz_instance_name}`, you'll see them all there.
- `name` is optional, but desirable (if it is missing, HASS will show the sensor as simply "MQTT Sensor").
- `unit_of_measurement` is optional, but desirable (if it is missing, HASS will refuse to display the line graph).

After this is done, all you need to do is:
1. Restart HASS to re-read the configuration;
1. Invoke "Configure UI" at HASS `Overview` panel;
1. Click on the plus button;
1. Click on `sensor` entry;
1. Click on the `Entity` dropdown, which should already have the sensor.
   Voila.

## Making Home Assistant recognize DZ thermostats

Add this block to `configuration.yaml`:

```
mqtt_eventstream:
  publish_topic: ${hass_publish_topic}

climate:
  - platform: mqtt
    name: "Family Room"
    current_temperature_topic: "${your_dz_instance_name}/thermostat/${dz_thermostat_name}"
    current_temperature_template: "{{ value_json.currentTemperature }}"
    mode_state_topic: "${your_dz_instance_name}/thermostat/${dz_thermostat_name}"
    mode_state_template: "{{ value_json.mode }}"
    modes:
      - "Cooling"
      - "Off"
      - "Heating"
    temperature_state_topic: "${your_dz_instance_name}/thermostat/${dz_thermostat_name}"
    temperature_state_template: "{{ value_json.setpointTemperature }}"
    hold_state_topic: "${your_dz_instance_name}/thermostat/${dz_thermostat_name}"
    hold_state_template: "{{ value_json.onHold }}"
    temp_step: 0.1
```

`${hass_publish_topic}` should be set to the same value as you set `${subscribe_topic}` in DZ configuration. Same goes for `${your_dz_instance_name}`, and `${dz_thermostat_name}`.

After all of the above is done, Home Assistant will have a thermostat
widget on the home screen which you can use to control a DZ zone.

### Special note: Units

DZ produces and expects all temperature values in
Centigrade (and all the values in SI Units, in general). My HASS
installation is configured to do that as well
(homeassistant/unit_system:metric), so everything just works. Advice
on how to configure things so HASS can produce and consume non-SI Unit
values is appreciated.

Curiously, [MQTT HVAC](https://www.home-assistant.io/integrations/climate.mqtt) does mention Fahrenheit once, but only as far as precision is concerned, it doesn't define the system behavior when Fahrenheit is chosen as the general platform measurement unit.

### Special note: principal limitation of Home Assistant thermostat control

DZ has from the very beginning been designed as a [multizone system](https://www.homeclimatecontrol.com/faq/temperature-zoning-and-climate-control#h.p_tjs44rqXagyY). Surprisingly, 20 years after its inception, this concept didn't get acknowledged by mainstream HVAC related projects, and even by many HVAC hardware manufacturers.

DZ does not, and will not, expose mode switching (heat/cool) to any system that does not specifically understand the concept of "multizone". Here's why:

- Multizone systems (such as DZ) realize that several zones may be controlled by the same HVAC unit, whereas non-multizone aware systems (such as HASS) do not;
- It would be awkward to allow HVAC units to be turned on or off with no regard to how other zones are commanding them at the moment;
- It would be somewhere between awkward and catastrophic to issue a mode change command to just one zone, or, even worse, issued different mode commands to different zones served by the same HVAC unit.

Until this restriction is lifted, the only things Home Assistant (and others) will be allowed to control would be just single thermostat properties.

---
PS: Compare to [how the Home Assistant integration is configured today](https://github.com/home-climate-control/dz/blob/master/docs/configuration/home-assistant.md).
