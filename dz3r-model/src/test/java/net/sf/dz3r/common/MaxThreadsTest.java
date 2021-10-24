package net.sf.dz3r.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MaxThreadsTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    @Order(1)
    void reportSystemConfig() {

        ThreadContext.push("system");
        assertThatCode(() -> {
            logger.info("CPU count reported: {}", Runtime.getRuntime().availableProcessors());

            // TL;DR: Set these two higher than defaults to force the JVM to dedicate more threads than Reactor thinks you deserve.

            logger.info("-Dreactor.schedulers.defaultPoolSize={}", System.getProperty("reactor.schedulers.defaultPoolSize"));
            logger.info("-Dreactor.schedulers.defaultBoundedElasticSize={}", System.getProperty("reactor.schedulers.defaultBoundedElasticSize"));

            logger.info("reactor-core default pool size: {}", Schedulers.DEFAULT_POOL_SIZE);
            logger.info("reactor-core default bounded elastic size: {}", Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE);
            logger.info("reactor-core default bounded elastic queue size: {}", Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE);

        }).doesNotThrowAnyException();
        ThreadContext.pop();
    }

    @ParameterizedTest
    @MethodSource("schedulersProvider")
    void maxThreadsParallel(Scheduler scheduler) {
        ThreadContext.push("parallel/" + scheduler.getClass().getSimpleName());
        var threads = new HashSet<String>();

        try {
            Flux.range(0, 1000)
                    .parallel()
                    .runOn(scheduler)
                    .doOnNext(ignored -> threads.add(Thread.currentThread().getName()))
                    .sequential()
                    .blockLast();

            var cpuCount = Runtime.getRuntime().availableProcessors();
            var threadCount = threads.size();

            logger.info("max threads: {}", threadCount);
            assertThat(threadCount).isGreaterThanOrEqualTo(cpuCount);

        } finally {
            scheduler.dispose();
            ThreadContext.pop();
        }
    }

    private static Stream<Scheduler> schedulersProvider() {
        return Stream.of(
                Schedulers.newParallel("newParallel.250", 250),
                Schedulers.newParallel("newParallel.500", 500),
                Schedulers.newBoundedElastic(250, 100_000, "newBoundedElastic.250"),
                Schedulers.newBoundedElastic(500, 100_000, "newBoundedElastic.500"),
                Schedulers.boundedElastic()
        );
    }
}
