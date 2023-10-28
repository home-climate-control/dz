package net.sf.dz3r.device.actuator;

import net.sf.dz3r.common.TestClock;
import net.sf.dz3r.device.DeviceState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractCqrsDeviceTest {

    @ParameterizedTest
    @MethodSource("getPaceStream")
    void pace(Flux<PaceTuple> source) {

        var clock = new TestClock();
        var device = new PaceTest("pt", clock, null, Duration.ofSeconds(30));

        source
                .doOnNext(t -> {
                    clock.setOffset(t.offset);
                    assertThat(device.limitRate(t.command).blockFirst()).isEqualTo(t.expected);
                })
                .blockLast();
    }

    private record PaceTuple(
            String command,
            Duration offset,
            String expected
    ) {

    }

    static Stream<Flux<PaceTuple>> getPaceStream() {

        return Stream.of(
                Flux.just(
                        new PaceTuple("A", Duration.ZERO, "A"), // Pace is 30 seconds
                        new PaceTuple("A", Duration.ofSeconds(40), "A"), // Beyond the window, should be let through
                        new PaceTuple("A", Duration.ofSeconds(50), null), // Falls within, swallowed
                        new PaceTuple("B", Duration.ofSeconds(51), "B") // Different command, let through
                )
        );
    }

    private class PaceTest extends AbstractCqrsDevice<String, String> {

        protected PaceTest(String id, Clock clock, Duration heartbeat, Duration pace) {
            super(id, clock, heartbeat, pace);
        }

        @Override
        protected void setStateSync(String command) {
            throw new IllegalStateException("shouldn't be used");
        }

        @Override
        protected String getCloseCommand() {
            throw new IllegalStateException("shouldn't be used");
        }

        @Override
        protected void closeSubclass() throws Exception {
            throw new IllegalStateException("shouldn't be used");
        }

        @Override
        public boolean isAvailable() {
            throw new IllegalStateException("shouldn't be used");
        }

        @Override
        public DeviceState<String> setState(String state) {
            throw new IllegalStateException("shouldn't be used");
        }
    }
}
