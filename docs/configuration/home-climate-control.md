home-climate-control
==
The root element of Home Climate Control configuration. Contains everything else.
The only literal property is `instance`, currently used only in the [console](./console.md), but will be used elsewhere to uniquely identify this system.
A good value for it would be the name of the host the instance runs on, **and** the configuration variant.

Example:

```yaml
home-climate-control:
  instance: rack-0-server-2-docker-wireless-only
```

---
[^^^ Configuration](./index.md)
