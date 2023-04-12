package net.sf.dz3r.device.actuator;

import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Scheduler;
import reactor.util.annotation.NonNull;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class AbstractSwitchTest {

    @Test
    void testFirst() {


        var s = new TestSwitch("A", null, null, null);

        var state = s.setState(true).block();

        assertThat(state).isTrue();
        assertThat(s.counter.get()).isEqualTo(1);
    }
    @Test
    void testNullDelay() {


        var s = new TestSwitch("A", null, null, null);

        s.setState(true).block();
        s.setState(true).block();

        assertThat(s.counter.get()).isEqualTo(2);
    }

    @Test
    void testIdenticalTooClose() {

        var clock = new TestClock(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        var rateAllowed = Duration.ofSeconds(1);

        var s = new TestSwitch("A", null, rateAllowed, clock);

        s.setState(true).block();

        clock.setOffset(Duration.ofMillis(500));
        var state = s.setState(true).block();

        assertThat(state).isTrue();
        assertThat(s.counter.get()).isEqualTo(1);
    }
    @Test
    void testIdenticalFarEnough() {

        var clock = new TestClock(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        var rateAllowed = Duration.ofSeconds(1);

        var s = new TestSwitch("A", null, rateAllowed, clock);

        var state1 = s.setState(true).block();
        assertThat(state1).isTrue();

        clock.setOffset(Duration.ofSeconds(2));
        var state2 = s.setState(true).block();

        assertThat(state2).isTrue();
        assertThat(s.counter.get()).isEqualTo(2);
    }
    @Test
    void testCloseButDifferent() {

        var clock = new TestClock(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        var rateAllowed = Duration.ofSeconds(1);

        var s = new TestSwitch("A", null, rateAllowed, clock);

        var state1 = s.setState(true).block();

        clock.setOffset(Duration.ofMillis(500));
        var state = s.setState(false).block();

        assertThat(state).isFalse();
        assertThat(s.counter.get()).isEqualTo(2);
    }
    @Test
    void testFarEnoughAndDifferent() {

        var clock = new TestClock(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        var rateAllowed = Duration.ofSeconds(1);

        var s = new TestSwitch("A", null, rateAllowed, clock);

        var state1 = s.setState(true).block();

        clock.setOffset(Duration.ofSeconds(2));
        var state = s.setState(false).block();

        assertThat(state).isFalse();
        assertThat(s.counter.get()).isEqualTo(2);
    }

    private static class TestSwitch extends AbstractSwitch<String> {

        private Boolean state;
        public final AtomicInteger counter = new AtomicInteger(0);

        protected TestSwitch(String address, Scheduler scheduler, Duration minDelay, Clock clock) {
            super(address, scheduler, minDelay, clock);
        }

        @Override
        protected void setStateSync(boolean state) throws IOException {
            this.state = state;
            counter.incrementAndGet();
        }

        @Override
        protected boolean getStateSync() throws IOException {
            return state;
        }
    }

    private static class TestClock extends Clock {

        private final Clock baseClock;
        private Duration offset = Duration.ofSeconds(0);

        private TestClock() {
            baseClock = Clock.systemUTC();
        }

        private TestClock(Clock baseClock) {
            this.baseClock = baseClock;
        }

        public void setOffset(@NonNull Duration offset) {
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
}
