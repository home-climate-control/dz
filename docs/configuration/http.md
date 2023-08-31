http
==

Integration that allows to make [HCC Remote](https://play.google.com/store/apps/details?id=net.sf.dz4.android.remote) to work.

> **NOTE:** This integration requires obtaining credentials, please drop a message to the [user forum](http://groups.google.com/group/home-climate-control) to get them.

Best explained by example:

```yaml
  connectors:
    - http:
        id: http-connector-appspot-v3
        uri: https://home-climate-control-v3.appspot.com/v3/data-source
        zones:
          - Workshop-east
          - Workshop-west
```

* `id`: Unique identifier this entity will be known as to the rest of the system.
* `uri`: Proxy URL
* `zones`: list of [zone](./zones.md) **names** for the moment, soon to be replaced with zone **identifiers** for improved configuration stability. Keep an eye on [issue #278](https://github.com/home-climate-control/dz/issues/278) not to miss this.

### Property of
* [connectors](./connectors.md)

---
[^^^ Configuration](./index.md)  
[^^^ connectors](./connectors.md)
