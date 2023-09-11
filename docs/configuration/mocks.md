mocks
==

The primary use for the mocks is to be able to debug the system configuration without jeopardizing expensive hardware.

This section follows the same syntax as [sensors & switches](./sensors-and-switches.md). Only switches are supported at the moment.

Example:

```yaml
mocks:
  - switches:
    - address: null-switch-fan-cluster
    - address: null-switch-economizer-workshop
    - address: null-switch-economizer-bedroom
    - address: null-switch-server-room
    - address: null-switch-unit1-mode
    - address: null-switch-unit1-running
    - address: null-switch-unit1-fan
    - address: null-switch-unit2-mode
    - address: null-switch-unit2-running
    - address: null-switch-unit2-fan
```

### Property of
* [home-climate-control](./home-climate-control.md)

---
[^^^ Configuration](./index.md)
