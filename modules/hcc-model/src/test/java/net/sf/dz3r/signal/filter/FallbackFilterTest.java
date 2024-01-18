package net.sf.dz3r.signal.filter;

import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.tools.agent.ReactorDebugAgent;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackFilterTest {

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    @Test
    void linkedHashMapOrder() {

        var map = new LinkedHashMap<String, Integer>();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        // What's the order now?
        map.put("a", 5);

        Iterator<Map.Entry<String, Integer>> i = map.entrySet().iterator();

        // Good, the order of initial insertion. Just what we need.

        assertThat(i.next().getKey()).isEqualTo("a");
        assertThat(i.next().getKey()).isEqualTo("b");
        assertThat(i.next().getKey()).isEqualTo("c");
    }

    @Test
    void test123() {

        String source1 = "A";
        String source2 = "B";
        String source3 = "C";
        var filter = new FallbackFilter<Integer, String>(List.of(source1, source2, source3));
        var now = Instant.now();

        var sequence = Flux.just(

                new Signal<>(now.plus(1, ChronoUnit.SECONDS), 1, source1), // comes through as OK
                new Signal<>(now.plus(2, ChronoUnit.SECONDS), 2, source2), // drop
                new Signal<>(now.plus(3, ChronoUnit.SECONDS), 3, source3), // drop

                new Signal<>(now.plus(4, ChronoUnit.SECONDS), 4, source1, Signal.Status.FAILURE_PARTIAL, new TimeoutException(source1)), // comes through as partial failure
                new Signal<>(now.plus(5, ChronoUnit.SECONDS), (Integer) null, source2, Signal.Status.FAILURE_TOTAL, new IOException(source2)), // drop
                new Signal<>(now.plus(6, ChronoUnit.SECONDS), 5, source3, Signal.Status.FAILURE_PARTIAL, new UnsupportedOperationException(source3)), // drop

                new Signal<>(now.plus(7, ChronoUnit.SECONDS), (Integer) null, source1, Signal.Status.FAILURE_TOTAL, new IllegalStateException(source1)), // drop
                new Signal<>(now.plus(8, ChronoUnit.SECONDS), (Integer) null, source2, Signal.Status.FAILURE_TOTAL, new IllegalStateException(source2)), // drop
                new Signal<>(now.plus(9, ChronoUnit.SECONDS), (Integer) null, source3, Signal.Status.FAILURE_TOTAL, new IllegalStateException(source3)), // comes through as total failure

                new Signal<>(now.plus(10, ChronoUnit.SECONDS), 6, source2) // comes through as partial failure
        );

        var result = filter.compute(sequence);

        StepVerifier.create(result)
                .assertNext(s -> {
                    assertThat(s.getValue()).isEqualTo(1);
                    assertThat(s.payload).isEqualTo(source1);
                    assertThat(s.status).isEqualTo(Signal.Status.OK);
                })
                .assertNext(s -> {
                    assertThat(s.getValue()).isEqualTo(4);
                    assertThat(s.payload).isEqualTo(source1);
                    assertThat(s.status).isEqualTo(Signal.Status.FAILURE_PARTIAL);
                    assertThat(s.error).isInstanceOf(TimeoutException.class);
                })
                .assertNext(s -> {
                    assertThat(s.getValue()).isNull();
                    assertThat(s.payload).isEqualTo(source3);
                    assertThat(s.status).isEqualTo(Signal.Status.FAILURE_TOTAL);
                    assertThat(s.error).isInstanceOf(IllegalStateException.class);
                })
                .assertNext(s -> {
                    assertThat(s.getValue()).isEqualTo(6);
                    assertThat(s.payload).isEqualTo(source2);
                    assertThat(s.status).isEqualTo(Signal.Status.FAILURE_PARTIAL);
                })
                .verifyComplete();
    }
}
