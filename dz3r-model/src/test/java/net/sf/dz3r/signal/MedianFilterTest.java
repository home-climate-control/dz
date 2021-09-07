package net.sf.dz3r.signal;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link MedianFilter}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
class MedianFilterTest {

    @Test
    void testEmpty() {

        Flux<Double> sequence = Flux.empty();
        List<Double> match = List.of();

        test(3, sequence, match);
    }

    @Test
    void test2just1() {

        var sequence = Flux.just(1d);
        var match = List.of(1d);

        test(2, sequence, match);
    }

    @Test
    void test3just1() {

        var sequence = Flux.just(1d);
        var match = List.of(1d);

        test(3, sequence, match);
    }

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

        var source = sequence
                .map(s -> new Signal<>(Instant.now(), s, (Void) null));
        var out = new DoubleMedianFilter(depth)
                .compute(source)
                .map(Signal::getValue);

        var result = out.collect(Collectors.toList()).block();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void someErrors() {

        var source = Flux.just(
                new Signal<>(Instant.now(), 2d, (Void)null),
                new Signal<>(Instant.now(), (Double)null, (Void)null, Signal.Status.FAILURE_TOTAL, new NullPointerException()),
                new Signal<>(Instant.now(), (Double)null, (Void)null, Signal.Status.FAILURE_TOTAL, new TimeoutException())
        );

        var out = new DoubleMedianFilter(3)
                .compute(source);

        StepVerifier.create(out)
                .assertNext(s -> assertThat(s.getValue()).isEqualTo(2d))
                .assertNext(s -> {
                    assertThat(s.getValue()).isEqualTo(2d);
                    assertThat(s.isOK()).isFalse();
                    assertThat(s.isError()).isFalse();
                    assertThat(s.error).isInstanceOf(NullPointerException.class);
                })
                .assertNext(s -> {
                    assertThat(s.getValue()).isEqualTo(2d);
                    assertThat(s.isOK()).isFalse();
                    assertThat(s.isError()).isFalse();
                    assertThat(s.error).isInstanceOf(TimeoutException.class);
                })
                .verifyComplete();
    }

    @Test
    void allErrors() {

        var source = Flux.just(
                new Signal<>(Instant.now(), (Double)null, (Void)null, Signal.Status.FAILURE_TOTAL, new NullPointerException()),
                new Signal<>(Instant.now(), (Double)null, (Void)null, Signal.Status.FAILURE_TOTAL, new TimeoutException())
        );

        var out = new DoubleMedianFilter(2)
                .compute(source);

        StepVerifier.create(out)
                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.isOK()).isFalse();
                    assertThat(s.isError()).isTrue();
                    assertThat(s.error).isInstanceOf(NullPointerException.class);
                })
                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.isOK()).isFalse();
                    assertThat(s.isError()).isTrue();
                    assertThat(s.error).isInstanceOf(TimeoutException.class);
                })
                .verifyComplete();
    }
}
