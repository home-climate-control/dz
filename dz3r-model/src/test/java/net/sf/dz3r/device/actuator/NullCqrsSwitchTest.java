package net.sf.dz3r.device.actuator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class NullCqrsSwitchTest {
    private final Logger logger = LogManager.getLogger();

    @Test
    void unlimited() throws InterruptedException {

        var s = new NullCqrsSwitch("unlimited");
        var result = Flux
                .range(0, 20)
                .map(ignore -> true)
                .map(s::setState)
                .doOnNext(state -> logger.info("state: {}", state))
                .blockLast();

        logger.info("done sending");

        // Let things settle down
        Thread.sleep(100);

        assertThat(s.getState().queueDepth).isZero();
    }
    @Test
    void limited() throws InterruptedException {

        var s = new NullCqrsSwitch("limited", Clock.systemUTC(), null, Duration.ofSeconds(1), null, null);
        var result = Flux
                .range(0, 20)
                .map(ignore -> true)
                .map(s::setState)
                .doOnNext(state -> logger.info("state: {}", state))
                .blockLast();

        logger.info("done sending");

        // Let things settle down
        Thread.sleep(100);

        assertThat(s.getState().queueDepth).isZero();
    }
}
