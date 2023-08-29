package net.sf.dz3.runtime.config;

import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CountDownLatch;

public class ShutdownHandler implements AutoCloseable {

    private final Logger logger = LogManager.getLogger();
    private final ConfigurationContext context;

    private final CountDownLatch stopGate = new CountDownLatch(1);

    public ShutdownHandler(ConfigurationContext context) {
        this.context = context;
    }

    @Override
    public void close() throws Exception {

        ThreadContext.push("shutdown");
        Marker m = new Marker("shutdown", Level.INFO);
        try {
            logger.warn("Shutting down");

            // Disable controls - console and WebUI
            logger.error("FIXME: shut off controls");

            // Stop directors so they don't interfere with anything anymore
            context.directors
                    .getFlux()
                    .parallel()
                    .runOn(Schedulers.boundedElastic())
                    .doOnNext(kv -> {
                        try {
                            kv.getValue().close();
                        } catch (Exception ex) {
                            apologize(ex);
                        }
                    })
                    .sequential()

                    // Need to wait for this, or things might go awry
                    .blockLast();

            m.checkpoint("disconnected directors");

            // Rest of operations can be done asynchronously until further notice - no need to wait for them,
            // but must wait before returning from this method

            // Shut off HVAC devices

            var hvacDevices = context.hvacDevices
                    .getFlux()
                    .parallel()
                    .runOn(Schedulers.boundedElastic())
                    .doOnNext(kv -> {
                        try {
                            kv.getValue().close();
                        } catch (Exception ex) {
                            apologize(ex);
                        }

                    });

            m.checkpoint("instructed HVAC devices to shut down");

            // Move dampers to "safe" position
            logger.error("FIXME: move dampers to safe position");

            // Close connectors
            logger.error("FIXME: close connectors");

            // Must have HVAC devices down by now

            hvacDevices
                    .sequential()
                    .blockLast();
            m.checkpoint("HVAC devices off");

            // Put switches into "safe" state, if provided
            logger.error("FIXME: put switches into safe state");

            // Cancel sensor stream subscriptions
            logger.error("FIXME: cancel sensor streams");

            // Deactivate MQTT and hardware drivers

            context.mqtt
                    .getFlux()
                    .parallel()
                    .runOn(Schedulers.boundedElastic())
                    .doOnNext(kv -> {
                        try {
                            kv.getValue().close();
                        } catch (Exception ex) {
                            apologize(ex);
                        }
                    })
                    .sequential()
                    .blockLast();

            logger.error("FIXME: deactivate 1-Wire");
            logger.error("FIXME: deactivate XBee");

        } catch (Throwable t) {
            apologize(t, Level.FATAL);
        } finally {
            logger.warn("done");
            m.close();
            ThreadContext.pop();
            stopGate.countDown();
        }
    }

    private void apologize(Throwable t) {
        apologize(t, Level.WARN);
    }

    private void apologize(Throwable t, Level level) {
        logger.log(level, "Unexpected exception, nothing we can do about it here", t);
    }

    public void await() throws InterruptedException {
        stopGate.await();
    }
}
