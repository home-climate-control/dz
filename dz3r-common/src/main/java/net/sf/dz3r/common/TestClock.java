package net.sf.dz3r.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class TestClock extends Clock {

    private final Clock baseClock;
    private Duration offset = Duration.ofSeconds(0);

    public TestClock() {
        baseClock = Clock.systemUTC();
    }

    public TestClock(Clock baseClock) {
        this.baseClock = baseClock;
    }

    public void setOffset(Duration offset) {
        this.offset = offset;
    }

    @Override
    public ZoneId getZone() {
        return baseClock.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return baseClock.withZone(zone);
    }

    @Override
    public Instant instant() {
        return baseClock.instant().plus(offset);
    }
}
