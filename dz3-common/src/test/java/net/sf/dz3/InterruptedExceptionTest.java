package net.sf.dz3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify <a href="https://rules.sonarsource.com/java/tag/multi-threading/RSPEC-2142">RSPEC-2142</a> rule.
 *
 * Bottomline: this rule is an annoyance related to Java implementation details that are never used in this project.
 * Presence or absence of {@code Thread.currentThread().interrupt()} does not alter the behavior.
 */
class InterruptedExceptionTest {

    private final Logger logger = LogManager.getLogger();

    /**
     * Make sure that interrupting the thread doesn't disrupt the control flow when it needs to exit.
     */
    @Test
    void needToBailOut() throws InterruptedException {

        var startGate = new CountDownLatch(1);
        var stopGate = new CountDownLatch(1);
        var started = new AtomicInteger();
        var interrupted = new AtomicInteger();
        var reinterrupted = new AtomicInteger();

        Thread t = new Thread(() -> {

            try {

                started.incrementAndGet();
                startGate.countDown();

                synchronized (this) {
                    try {

                        logger.info("about to start waiting");
                        wait();

                    } catch (InterruptedException e) {

                        interrupted.incrementAndGet();
                        logger.info("interrupted");

                        Thread.currentThread().interrupt();

                        reinterrupted.incrementAndGet();
                        logger.info("reinterrupted");
                    }
                }
            } finally {
                stopGate.countDown();
            }
        });

        t.start();

        logger.info("waiting for the thread to start");
        startGate.await();

        logger.info("thread started, interrupting");
        t.interrupt();

        logger.info("waiting for the thread to stop");
        stopGate.await();

        logger.info("making assertions");
        assertThat(started.get()).isEqualTo(1);
        assertThat(interrupted.get()).isEqualTo(1);
        assertThat(reinterrupted.get()).isEqualTo(1);
    }

    /**
     * Make sure that interrupting the thread doesn't disrupt the control flow when it needs to keep going.
     */
    @Test
    void needToKeepGoing() throws InterruptedException {

        var startGate = new CountDownLatch(1);
        var stopGate = new CountDownLatch(1);
        var started = new AtomicInteger();
        var interrupted = new AtomicInteger();
        var reinterrupted = new AtomicInteger();

        Thread t = new Thread(() -> {

            try {

                started.incrementAndGet();
                startGate.countDown();

                while (true) {

                    synchronized (this) {
                        try {

                            logger.info("about to start waiting");
                            wait();

                        } catch (InterruptedException e) {

                            interrupted.incrementAndGet();
                            logger.info("interrupted/{}", interrupted.get());

                            Thread.currentThread().interrupt();

                            reinterrupted.incrementAndGet();
                            logger.info("reinterrupted/{}", reinterrupted.get());

                            if (interrupted.get() > 4) {
                                logger.info("enough, bailing out");
                                return;
                            }
                        }
                    }
                }

            } finally {
                stopGate.countDown();
            }
        });

        t.start();

        logger.info("waiting for the thread to start");
        startGate.await();

        logger.info("thread started, interrupting");
        t.interrupt();
        t.interrupt();
        t.interrupt();
        t.interrupt();
        t.interrupt();
        t.interrupt();

        logger.info("waiting for the thread to stop");
        stopGate.await();

        logger.info("making assertions");
        assertThat(started.get()).isEqualTo(1);
        assertThat(interrupted.get()).isEqualTo(5);
        assertThat(reinterrupted.get()).isEqualTo(5);
    }
}
