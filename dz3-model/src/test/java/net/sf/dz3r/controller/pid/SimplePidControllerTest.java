package net.sf.dz3r.controller.pid;

import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class SimplePidControllerTest {

    private final Random rg = new Random();

    @Test
    void testPSimple() throws InterruptedException {
        testP(new SimplePidController<>("simple", 0, 1, 0, 0, 0));
    }

    private void testP(ProcessController<Double, Double, Void> pc) {

        ThreadContext.push("testP/" + pc.getClass().getName());

        try {

            // Feel free to push this north of 5,000,000
            int COUNT = 5_000;

            var sourceSequence = new ArrayList<Double>();
            var signalSequence = new ArrayList<Signal<ProcessController.Status<Double>, Void>>();

            var timestamp = new AtomicLong(Instant.now().toEpochMilli());
            Flux<Signal<Double, Void>> sourceFlux = Flux.generate(
                            rg::nextDouble,
                    (state, sink) -> {
                        state = rg.nextDouble();
                        sink.next(state);
                        sourceSequence.add(state);
                        return state;
                    })
                    .map(d -> new Signal<Double, Void>(
                            Instant.ofEpochMilli(timestamp.incrementAndGet()), (Double)d))
                    .take(COUNT);

            var signalFlux = pc.compute(sourceFlux);

            signalFlux
                    .doOnNext(signalSequence::add)
                    .subscribe()
                    .dispose();

            assertThat(sourceSequence).hasSize(COUNT);
            assertThat(signalSequence).hasSize(COUNT);

            int offset = 0;
            for (var v : sourceSequence) {
                assertThat(v).isEqualTo(signalSequence.get(offset++).getValue().signal);
            }

            assertThat(signalSequence.get(0).getValue()).isInstanceOf(PidController.PidStatus.class);

        } finally {
            ThreadContext.pop();
        }
    }
}
