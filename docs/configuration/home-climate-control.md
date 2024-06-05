home-climate-control
==
The root element of Home Climate Control configuration. Contains everything else.

### instance

This only literal property is a mandatory element that is used to identify the currently running HCC instance to many systems, in particular, [connectors](./connectors.md).
The recommended value for it is a composite of the host this instance runs on, the method used to run it, and configuration variant.

#### Example 1
```yaml
home-climate-control:
  instance: rack-0-server-2-docker-wireless-only
```

#### Example 2
```yaml
home-climate-control:
  instance: workshop-pi3b-vnc-pimoroni-hat
```

> ***Hint:*** With SpringBoot, Docker and Quarkus runners, you can use a profile file that consists just of this element, and use it along with other profiles.

### measurement-units

For now, only temperature unit is configurable. This property is optional, default value is `C` (degrees Celsius), the only other allowed value is `F` (degrees Fahrenheit).

> **NOTE:** This setting impacts the way all the configuration files are read, and the way all the information is displayed. Be very careful changing this value on the fly, everything will change - the room temperature of neither 72C nor 22F is not comfortable.

You can change the way the information is displayed without changing the way the configuration is stored, there are console switches for that.

Regardless of the value here, all internal calculations, logging, and telemetry will be conducted in [SI units](https://en.wikipedia.org/wiki/International_System_of_Units). 

#### Example
```yaml
home-climate-control:
  measurement-units:
    temperature: F
```

**P.S.** Consider using a default :)

![countries that use Fahrenheit](https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Countries_that_use_Fahrenheit.svg/320px-Countries_that_use_Fahrenheit.svg.png)

**P.P.S.** This setting will be recognized but for now rejected - it's been deprioritized to give more time to other things. If it is important for you, please voice your support at [#315](https://github.com/home-climate-control/dz/issues/315).

---
[^^^ Configuration](./index.md)
