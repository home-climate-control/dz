# Google Calendar Schedule Integration Syntax

Context: [home-climate-control.schedule.google-calendar](./schedule.md#google-calendar) integration.  
See also: [deprecated schedule integration syntax](./schedule-syntax-deprecated.md)

---

HCC reads the `title` (contains period name), `description` (contains period settings), `start`, `end`, `all day`, `recurrence` calendar fields and ignores the rest.

**IMPORTANT:** historically, different calendars are used for heating and cooling, and the heating or cooling mode is set outside of the calendar, so you won't find any mention of it here. This may change in the future.

# General Syntax

Until the [old syntax](./schedule-syntax-deprecated.md) is retired the `description` field will be attempted to be parsed into period settings, if that fails,
then the old syntax will be attempted to be parsed (having issued a `WARN` level message in the log), and only then the scheduler will give up.

## Title field

Contains the period name. No restrictions.  
 
## Description field

Contains the period settings. Best explained by example:

```yaml
# Comments are allowed in the event description text 
enabled: true | false # optional, defaults to true
voting: true | false # optional, defaults to true
setpoint: decimal number # mandatory
dump-priority: integer number # optional, defaults to 0
economizer: # optional section, defaults to "not present"
  changeover-delta: decimal number # mandatory
  target-temperature: decimal number # mandatory
  keep-hvac-on: true | false # optional, defaults to true
  max-power: decimal number between 0 and 1 # optional, defaults to 1
```

### enabled

The value of `true` mean that the zone is ON during this period. This is the default behavior.

The value of `false`` mean that the zone is OFF during this period. You have to explicitly specify this.

Setpoint value is ignored for a disabled zone, to make it easier to do one-off edits. However, it is still required.

### voting

The value of `true` means that if the zone is calling for heat or cool during this period, it will cause the HVAC unit to turn on. This is the default behavior.

The value of `false` means that the HVAC unit will only turn on if any other **voting** zone calls for heat or cool.

### setpoint

Setpoint temperature. Numerical value. There is no default for the setpoint.


### dump-priority

Unused for now, but allowed to be present.

### economizer

### economizer.changeover-delta

Specifies temperature difference between indoor and outdoor temperature necessary to turn the device on.

### economizer.target-temperature

When this temperature is reached, the economizer is shut off.

### economizer.keep-hvac-on

The value of `true` means that the main HVAC unit for this zone will be kept on even if the economizer is on. This maximizes comfort.

The value of `false` means that when the conditions are suitable for the economizer to turn on, the main HVAC unit will be kept off. This maximizes energy savings.

### economizer.max-power

Specifies the maximum amount of power allowed to be applied to a variable output HVAC device acting as an economizer (example: max speed for a fan).

The value will be ignored with a log warning if the configured economizer device doesn't support variable output.

## Start and end time, recurrence

**IMPORTANT:** Period can't cross midnight. This is an ancient design limitation, [comment on issue #5](https://github.com/home-climate-control/dz/issues/5) if this bothers you.

**IMPORTANT**: Google Calendar has a bug where recurrent events ending "never", in actuality, expire 2 years after the first event - but only under some circumstances. Don't forget to check your schedule into the future. Workaround: duplicate the event, and adjust the start date.

Other than that, self-explanatory.

---
[^^^ Configuration](./index.md)  
