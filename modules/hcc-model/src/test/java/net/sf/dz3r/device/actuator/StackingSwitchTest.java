package net.sf.dz3r.device.actuator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class StackingSwitchTest {

    private final Logger logger = LogManager.getLogger();

    /**
     * Make sure redundant requests to get the same virtual switch do produce the same switch.
     */
    @Test
    void getSwitch() {

        var target = new NullSwitch("null");
        var stack = new StackingSwitch("ss", target);

        var s0a = stack.getSwitch("s0");
        var s0b = stack.getSwitch("s0");
        var s1 = stack.getSwitch("s1");

        assertThat(s0a).isSameAs(s0b);
        assertThat(s0a).isNotSameAs(s1);
        assertThat(s0b).isNotSameAs(s1);
    }

    /**
     * Test the stacking switch with just one virtual switch.
     */
    @Test
    void lifecycle1() {

        var target = new NullSwitch("null");
        var stack = new StackingSwitch("ss", target);

        var s0 = stack.getSwitch("s0");

        var targetResult = new ArrayList<Boolean>();

        target
                .getFlux()
                .filter(s -> s.getValue().requested != null)
                .subscribe(s -> targetResult.add(s.getValue().requested));

        var groupSequence = Flux.concat(

                s0.setState(true),

                // target turning true here

                s0.setState(true),

                s0.setState(false)

                // target must be off now
                // total count: on twice, off once
        );

        var stateTrue = new AtomicInteger();
        var stateFalse = new AtomicInteger();

        run(groupSequence, targetResult, stateTrue, stateFalse);

        assertThat(stateTrue.get()).isEqualTo(2);
        assertThat(stateFalse.get()).isEqualTo(1);
    }

    private void run(Flux<Boolean> groupSequence, List<Boolean> targetResult, AtomicInteger stateTrue, AtomicInteger stateFalse) {

        groupSequence
                .doOnNext(s -> logger.debug("s={}", s))
                .blockLast();

        Flux.fromIterable(targetResult)
                .doOnNext(s -> logger.info("state: {}", s))
                .doOnNext(s -> {
                    if (s) {
                        stateTrue.incrementAndGet();
                    } else {
                        stateFalse.incrementAndGet();
                    }
                })
                .blockLast();
    }

    /**
     * Test the stacking switch with three virtual switches.
     */
    @Test
    void lifecycle3() {

        var target = new NullSwitch("null");
        var stack = new StackingSwitch("ss", target);

        var s0 = stack.getSwitch("s0");
        var s1 = stack.getSwitch("s1");
        var s2 = stack.getSwitch("s2");

        var targetResult = new ArrayList<Boolean>();

        target
                .getFlux()
                .filter(s -> s.getValue().requested != null)
                .subscribe(s -> targetResult.add(s.getValue().requested));

        var groupSequence = Flux.concat(

                s0.setState(true),

                // target turning true here

                s1.setState(true),
                s2.setState(true),

                s0.setState(false),
                s1.setState(false),

                // target is still true

                s0.setState(false),
                s1.setState(false),
                s2.setState(false)

                // target must be off now
                // total count: on as many times as "on" commands were received by all switches, off once
        );

        var stateTrue = new AtomicInteger();
        var stateFalse = new AtomicInteger();

        run(groupSequence, targetResult, stateTrue, stateFalse);

        assertThat(stateTrue.get()).isEqualTo(7);
        assertThat(stateFalse.get()).isEqualTo(1);
    }
}
