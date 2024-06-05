# Google Calendar Schedule Integration Syntax - deprecated

Context: [home-climate-control.schedule.google-calendar](./schedule.md#google-calendar) integration.

> **NOTE:** this syntax is about to be retired and replaced by [YAML syntax](./schedule-syntax.md).

---

HCC reads the `title` (contains period description), `start`, `end`, `all day`, `recurrence` calendar fields and ignores the rest.

**IMPORTANT:** historically, different calendars are used for heating and cooling, and the heating or cooling mode is set outside of the calendar, so you won't find any mention of it here. This may change in the future.

# General Syntax

## Title field

Contains a period name followed by a colon, and several sections, separated by `,` or `;`:

* on/off
* voting
* setpoint
* dump priority

### On/Off

String literals `on` and `enabled` mean that the zone is ON during this period. This is the default behavior.

String literals `off` and `disabled` mean that the zone is OFF during this period. You have to explicitly specify this.

Setpoint value is ignored for a disabled zone, to make it easier to do one-off edits. However, it is still required.

### Voting

String literal `voting` means that if the zone is calling for heat or cool during this period, it will cause the HVAC unit to turn on. This is the default behavior.

String literal `non-voting` or `not voting` means that the HVAC unit will only turn on if any other **voting** zone calls for heat or cool.

### Setpoint

String literal `setpoint` or `temperature` followed by space, then a numerical value, and optional `C` or `F` unit modifier specifies the setpoint temperature.

There is no default for the setpoint.

Default temperature unit is `C` (degrees Celsius).

## Start and end time, recurrence

**IMPORTANT:** Period can't cross midnight. This is an ancient design limitation, [comment on issue #5](https://github.com/home-climate-control/dz/issues/5) if this bothers you.

**IMPORTANT**: Google Calendar has a bug where recurrent events ending "never", in actuality, expire 2 years after the first event - but only under some circumstances. Don't forget to check your schedule into the future. Workaround: duplicate the event, and adjust the start date.

Other than that, self-explanatory.

# Examples

### Away: setpoint 32C, not voting

Period name is `Away`  
Setpoint is at 32°C  
This zone will **not** turn on the HVAC if it calls for heat or cool because it is `non-voting`, but its setpoint will be satisfied if any other voting zone does.

### Away: setpoint 32, not voting

Same as the above, but the temperature unit defaults to degrees Celsius.

### Away: setpoint 90F, not voting

Same as the above, but the temperature unit is explicitly specified to be degrees Fahrenheit.

### Away: setpoint 32, off

Same as the above, but the zone will not participate in HVAC exchange, dampers will stay closed no matter what happens to the rest of the house. This zone will appear grayed out on Swing and mobile consoles.

### Night: setpoint 24

Period name is `Night`  
Setpoint is at 24°C  
Zone **will** turn on the HVAC if it calls for heat or cool, and it will be turned off when all other voting zones that it serves are satisfied.

---
[^^^ Configuration](./index.md)  
