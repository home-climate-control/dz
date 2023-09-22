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


---
[^^^ Configuration](./index.md)
