package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MedianSetFilterTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void test3x3() {

        DoubleMedianSetFilter<Integer> filter3 = new DoubleMedianSetFilter<>(3);
        var now = Instant.now();

        var s1a = new Signal<>(now, 1d, 1);
        var s1b = new Signal<>(now.plus(5, ChronoUnit.SECONDS), 2d, 1);
        var s1c = new Signal<>(now.plus(10, ChronoUnit.SECONDS), 8d, 1);

        var s2a = new Signal<>(now.plus(2, ChronoUnit.SECONDS), 2d, 2);
        var s2b = new Signal<>(now.plus(4, ChronoUnit.SECONDS), 3d, 2);
        var s2c = new Signal<>(now.plus(6, ChronoUnit.SECONDS), 4d, 2);

        var s3a = new Signal<>(now.plus(3, ChronoUnit.SECONDS), 4d, 3);
        var s3b = new Signal<>(now.plus(8, ChronoUnit.SECONDS), 5d, 3);
        var s3c = new Signal<>(now.plus(12, ChronoUnit.SECONDS), 6d, 3);

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
                .doOnNext(s -> logger.warn("signal: {}", s.getValue()));

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
}
