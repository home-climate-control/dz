package net.sf.dz3r.scheduler.gcal.v3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Disabled("Enable if you have the right credentials")
class GCalScheduleUpdaterTest {

    private final Logger logger = LogManager.getLogger();

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    @Test
    void breathe() throws InterruptedException {

        var start = new AtomicLong();

        var u = new GCalScheduleUpdater(Map.of(
                "Kitchen", "DZ Schedule: Kitchen",
                "Family Room", "DZ Schedule: Family Room",
                "Theater Room", "DZ Schedule: Theater Room"
        ));

        u.update()
                .take(3)
                .publishOn(Schedulers.boundedElastic())
                .doOnSubscribe(s -> {
                    start.set(Instant.now().toEpochMilli());
                })
                .doOnNext(s -> logger.info("item: {}", s))
                .doOnComplete(() -> {
                    logger.info("Completed in {}ms", Duration.between(Instant.ofEpochMilli(start.get()), Instant.now()).toMillis());
                })
                .blockLast();

        logger.info("Done.");
    }
}
