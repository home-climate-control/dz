http
==

Integration that allows to make [HCC Remote](https://play.google.com/store/apps/details?id=net.sf.dz4.android.remote) to work.

> **NOTE:** This integration will be inactive unless included into [directors.connectors](./directors.md).

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

* `id`: Unique identifier this entity will be known as to the rest of the system. In particular, it is used by [directors.connectors](./directors.md).
* `uri`: Proxy URL
* `zones`: list of [zone.id](./zones.md#id)s to report and accept commands for.

### Property of
* [connectors](./connectors.md)

---
[^^^ Configuration](./index.md)  
[^^^ connectors](./connectors.md)
