package net.sf.dz3r.device.actuator;

import net.sf.dz3r.common.TestClock;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Scheduler;

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


        var s = new TestSwitch("A", false, null, null, null);

        var state = s.setState(true).block();

        assertThat(state).isTrue();
        assertThat(s.counter.get()).isEqualTo(1);
    }
    @Test
    void testNullDelay() {


        var s = new TestSwitch("A", false, null, null, null);

        s.setState(true).block();
        s.setState(true).block();

        assertThat(s.counter.get()).isEqualTo(2);
    }

    @Test
    void testIdenticalTooClose() {

        var clock = new TestClock(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        var rateAllowed = Duration.ofSeconds(1);

        var s = new TestSwitch("A", false, null, rateAllowed, clock);

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

        var s = new TestSwitch("A", false, null, rateAllowed, clock);

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

        var s = new TestSwitch("A", false, null, rateAllowed, clock);

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

        var s = new TestSwitch("A", false, null, rateAllowed, clock);

        var state1 = s.setState(true).block();

        clock.setOffset(Duration.ofSeconds(2));
        var state = s.setState(false).block();

        assertThat(state).isFalse();
        assertThat(s.counter.get()).isEqualTo(2);
    }

    private static class TestSwitch extends AbstractSwitch<String> {

        private Boolean state;
        public final AtomicInteger counter = new AtomicInteger(0);

        protected TestSwitch(String address, boolean optimistic, Scheduler scheduler, Duration pace, Clock clock) {
            super(address, optimistic, scheduler, pace, clock);
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
}
