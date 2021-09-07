package net.sf.dz3r.signal;

import net.sf.dz3r.signal.hvac.DoubleMedianFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link MedianFilter}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
class MedianFilterTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void test3() {

        var sequence = Flux.just(1d, 2d, 3d, 4d, 5d);
        var match = List.of(1d, 1.5d, 2d, 3d, 4d);

        test(3, sequence, match);
    }

    @Test
    void test3repeated2() {

        var sequence = Flux.just(1d, 2d, 3d, 4d, 4d);
        var match = List.of(1d, 1.5d, 2d, 3d, 4d);

        test(3, sequence, match);
    }

    @Test
    void test3repeated3() {

        var sequence = Flux.just(1d, 2d, 3d, 4d, 4d, 4d);
        var match = List.of(1d, 1.5d, 2d, 3d, 4d, 4d);

        test(3, sequence, match);
    }

    @Test
    void test5() {

        var sequence = Flux.just(1d, 2d, 3d, 4d, 5d, 6d, 7d);
        var match = List.of(1d, 1.5d, 2d, 2.5d, 3d, 4d, 5d);

        test(5, sequence, match);
    }

    @Test
    void test5repeated2() {

        var sequence = Flux.just(1d, 2d, 3d, 4d, 5d, 6d, 7d, 7d);
        var match = List.of(1d, 1.5d, 2d, 2.5d, 3d, 4d, 5d, 6d);

        test(5, sequence, match);
    }

    @Test
    void test5repeated3() {

        var sequence = Flux.just(1d, 2d, 3d, 4d, 5d, 6d, 7d, 7d, 7d);
        var match = List.of(1d, 1.5d, 2d, 2.5d, 3d, 4d, 5d, 6d, 7d);

        test(5, sequence, match);
    }

    @Test
    void test2() {

        var sequence = Flux.just(1d, 2d, 3d, 4d, 5d);
        var match = List.of(1d, 1.5d, 2.5d, 3.5d, 4.5d);

        test(2, sequence, match);
    }

    @Test
    void test2repeated2() {

        var sequence = Flux.just(1d, 2d, 3d, 4d, 4d);
        var match = List.of(1d, 1.5d, 2.5d, 3.5d, 4d);

        test(2, sequence, match);
    }

    @Test
    void test4() {

        var sequence = Flux.just(1d, 2d, 3d, 4d, 5d, 6d, 7d);
        var match = List.of(1d, 1.5d, 2d, 2.5d, 3.5d, 4.5d, 5.5d);

        test(4, sequence, match);
    }

    @Test
    void test4repeated2() {

        var sequence = Flux.just(1d, 2d, 3d, 4d, 5d, 6d, 7d, 7d);
        var match = List.of(1d, 1.5d, 2d, 2.5d, 3.5d, 4.5d, 5.5d, 6.5d);

        test(4, sequence, match);
    }

    private void test(int depth, Flux<Double> sequence, List<Double> expected) {

        ThreadContext.push("test(" + depth + ")");

        try {

            logger.info("Depth {}, expected {}", depth, expected);

            var source = sequence
                    .map(s -> new Signal<>(Instant.now(), s, (Void) null));
            var out = new DoubleMedianFilter(depth)
                    .compute(source)
                    .map(Signal::getValue);

            var result = out.collect(Collectors.toList()).block();

            assertThat(result).isEqualTo(expected);

        } finally {
            ThreadContext.pop();
        }
    }
}
