schedule
==

Integration with scheduling services.

Best explained by example:

```yaml
  schedule:
    google-calendar:
    - zone: bedroom-master
      calendar: "HCC Schedule: Master Bedroom"
```

### google-calendar

Integration with [Google Calendar](https://calendar.google.com/).

Authentication is handled implicitly with [OAuth 2.0](https://oauth.net/2/).

* `zone`: The [zone.id](./zones.md#id) of the zone the schedule needs to be applied to. Using a YAML anchor here would be a good idea.
* `calendar`: The name of the calendar that contains the schedule.

### Property of
* [home-climate-control](./home-climate-control.md)

### Used in
* [directors](./directors.md) - implicitly

---
[^^^ Configuration](./index.md)
