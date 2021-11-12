package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MedianSetFilterTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void test3x3() {

        DoubleMedianSetFilter<Integer> filter3 = new DoubleMedianSetFilter<>(3);
        var now = Instant.now();

        var channel1 = 1;
        var channel2 = 2;
        var channel3 = 3;

        var s1a = new Signal<>(now, 1d, channel1);
        var s1b = new Signal<>(now.plus(5, ChronoUnit.SECONDS), 2d, channel1);
        var s1c = new Signal<>(now.plus(10, ChronoUnit.SECONDS), 8d, channel1);

        var s2a = new Signal<>(now.plus(2, ChronoUnit.SECONDS), 2d, channel2);
        var s2b = new Signal<>(now.plus(4, ChronoUnit.SECONDS), 3d, channel2);
        var s2c = new Signal<>(now.plus(6, ChronoUnit.SECONDS), 4d, channel2);

        var s3a = new Signal<>(now.plus(3, ChronoUnit.SECONDS), 4d, channel3);
        var s3b = new Signal<>(now.plus(8, ChronoUnit.SECONDS), 5d, channel3);
        var s3c = new Signal<>(now.plus(12, ChronoUnit.SECONDS), 6d, channel3);

        var sequence = Flux.just(
                        s1a, s1b, s1c,
                        s2a, s2b, s2c,
                        s3a, s3b, s3c
                )
                .sort(this::sortByTimestamp);

        // The actual sequence will be:

        // @00 (1, -, -) => 1
        // @02 (1, 2, -) => 1.5
        // @03 (1, 2, 4) => 2
        // @04 (1, 3, 4) => 3
        // @05 (2, 3, 4) => 3
        // @06 (2, 4, 4) => 4
        // @08 (2, 4, 5) => 4
        // @10 (8, 4, 5) => 5
        // @12 (8, 4, 6) => 6

        var result = filter3
                .compute(sequence)
                .doOnNext(s -> logger.info("signal: {}", s.getValue()));

        StepVerifier.create(result)
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(1d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(1.5d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(2d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(3d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(3d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(4d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(4d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(5d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(6d))
                .verifyComplete();
    }

    private int sortByTimestamp(Signal<?, ?> s1, Signal<?, ?> s2) {
        return s1.timestamp.compareTo(s2.timestamp);
    }

    @Test
    void wrapper() {

        // Need to stagger the signals so that they get to the verifier in the right order

        var s1a = Flux.just(1d);
        var s1b = Flux.just(2d).delayElements(delay(5));
        var s1c = Flux.just(8d).delayElements(delay(10));

        var f1 = Flux
                .merge(s1a, s1b, s1c)
                .map(d -> new Signal<Double, Void>(Instant.now(), d));

        var s2a = Flux.just(2d).delayElements(delay(2));
        var s2b = Flux.just(3d).delayElements(delay(4));
        var s2c = Flux.just(4d).delayElements(delay(6));

        var f2 = Flux
                .merge(s2a, s2b, s2c)
                .map(d -> new Signal<Double, Void>(Instant.now(), d));

        var s3a = Flux.just(4d).delayElements(delay(3));
        var s3b = Flux.just(5d).delayElements(delay(8));
        var s3c = Flux.just(6d).delayElements(delay(12));

        var f3 = Flux
                .merge(s3a, s3b, s3c)
                .map(d -> new Signal<Double, Void>(Instant.now(), d));

        var result = DoubleMedianSetFilter
                .compute(Set.of(f1, f2, f3))
                .doOnNext(s -> logger.info("signal: {}", s.getValue()));

        // Calculation is identical to the one above, except the timing is different

        StepVerifier.create(result)
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(1d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(1.5d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(2d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(3d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(3d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(4d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(4d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(5d))
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(6d))
                .verifyComplete();
    }

    private Duration delay(long factor) {
        // Easier to adjust the timing here to make the test case tolerant to slow boxes than fix it all around
        return Duration.ofMillis(factor * 10);
    }
}
