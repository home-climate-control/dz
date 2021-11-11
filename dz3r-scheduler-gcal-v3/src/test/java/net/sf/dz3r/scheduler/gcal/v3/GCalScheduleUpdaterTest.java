package net.sf.dz3r.scheduler.gcal.v3;

import net.sf.dz3r.model.ZoneSettings;
import net.sf.dz3r.scheduler.SchedulePeriod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

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
                "Theater Room", "DZ Schedule: Theater Room",
                "Workshop", "DZ Schedule: Workshop"
        ));

        var accumulator = new LinkedHashMap<String, SortedMap<SchedulePeriod, ZoneSettings>>();
        u.update()
                .take(4)
                .publishOn(Schedulers.boundedElastic())
                .doOnSubscribe(s -> {
                    start.set(Instant.now().toEpochMilli());
                })
                .doOnNext(s -> logger.info("item: {}", s))
                .doOnComplete(() -> {
                    logger.info("Completed in {}ms", Duration.between(Instant.ofEpochMilli(start.get()), Instant.now()).toMillis());
                })
                .doOnNext(kv -> accumulator.put(kv.getKey(), kv.getValue()))
                .blockLast();

        logger.info("Done.");

        assertThat(accumulator).hasSize(4);
    }
}
