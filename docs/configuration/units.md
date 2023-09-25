units
==

The purpose of this property is to tell the rest of the system how to treat the hardware it is connected to.

Best explained by example:

```yaml
  units:
    - single-stage:
        - id: unit1
        - id: fan-panel
    - multi-stage:
        - id: fan-cluster
          stages:
            - 10
            - 30
            - 50
```

* `single-stage`: 99% of practically encountered units, on/off.
* `multi-stage`: work in progress, stay tuned. 

### Property of
* [home-climate-control](./home-climate-control.md)

### Used in
* [directors](./directors.md)

---
[^^^ Configuration](./index.md)
