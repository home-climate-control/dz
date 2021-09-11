package net.sf.dz3r.scheduler.gcal.v3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Disabled("Enable if you have the right credentials")
class GCalScheduleUpdaterTest {

    private final Logger logger = LogManager.getLogger();

    @BeforeAll
    static void init() {
        ReactorDebugAgent.init();
    }

    @Test
    void breathe() throws InterruptedException {

        var complete = new CountDownLatch(1);

        var u = new GCalScheduleUpdater(Map.of(
                "Kitchen", "DZ Schedule: Kitchen",
                "Family Room", "DZ Schedule: Family Room",
                "Theater Room", "DZ Schedule: Theater Room"
        ));

        u.update()
                .log()
                .take(3)
                .publishOn(Schedulers.boundedElastic())
                .doOnComplete(() -> {
                        logger.info("Completed");
                        complete.countDown();
                })
                .subscribe(s -> logger.info("item: {}", s));

        logger.info("Awaiting completion...");
        complete.await();
        logger.info("Done.");
    }
}
