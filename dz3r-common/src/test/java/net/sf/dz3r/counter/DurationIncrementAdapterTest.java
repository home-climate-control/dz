package net.sf.dz3r.counter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DurationIncrementAdapterTest {

    private final Logger logger = LogManager.getLogger();

    @ParameterizedTest
    @MethodSource("uptimeFluxProvider")
    void testTerminated(Flux<Integer> uptime) {

        var inc = new TimeUsageCounter(Duration.ZERO, Duration.ZERO);
        var adapter = new DurationIncrementAdapter();
        var source = uptime
                .doOnNext(s -> logger.debug("absolute/in: {}", s))
                .map(Duration::ofSeconds);

        var increments = adapter
                .split(source)
                .doOnNext(s -> logger.info("split: {}", s.getSeconds()));

        var result = inc.consume(increments)
                .doOnNext(s -> logger.warn("absolute/out: {}", s.current().getSeconds()));

        StepVerifier
                .create(result)
                .assertNext(s -> assertThat(s.current().getSeconds()).isEqualTo(100))
                .assertNext(s -> assertThat(s.current().getSeconds()).isEqualTo(200))
                .assertNext(s -> assertThat(s.current().getSeconds()).isEqualTo(300))
                .verifyComplete();
    }

    @Test
    void isMonotonous() {

        var adapter = new DurationIncrementAdapter();
        var source = Flux.just(
                        100,
                        50)
                .doOnNext(s -> logger.debug("absolute/in: {}", s))
                .map(Duration::ofSeconds);

        var result = adapter
                .split(source)
                .doOnNext(s -> logger.info("split: {}", s.getSeconds()));

        StepVerifier
                .create(result)
                .assertNext(s -> assertThat(s.getSeconds()).isEqualTo(100))
                .expectErrorMatches(t -> t instanceof IllegalArgumentException && t.getMessage().equals("lastKnown=PT1M40S, snapshot=PT50S, not monotonous"))
                .verify();
    }

    private static Stream<Flux<Integer>> uptimeFluxProvider() {
        return Stream.of(
                Flux.just(
                        100,
                        200,
                        300),
                Flux.just(
                        100,
                        200,
                        300,
                        0)
        );
    }
}
