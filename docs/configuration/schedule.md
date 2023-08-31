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

* `zone`: The [zone](./zones.md) the schedule needs to be applied to.
* `calendar`: The name of the calendar that contains the schedule.

### Property of
* [home-climate-control](./home-climate-control.md)

### Used in
* [directors](./directors.md) - implicitly

---
[^^^ Configuration](./index.md)
