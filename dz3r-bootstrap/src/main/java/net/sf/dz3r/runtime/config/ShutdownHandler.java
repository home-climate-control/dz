package net.sf.dz3r.runtime.config;

import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.scheduler.Schedulers;

public class ShutdownHandler implements AutoCloseable {

    private final Logger logger = LogManager.getLogger();
    private final ConfigurationContext context;


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

            // Close connectors

            context.connectors
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

            m.checkpoint("stopped connectors");

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

            m.checkpoint("stopped directors");

            // Same for the schedule
            logger.error("FIXME: stop the scheduler");

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

            m.checkpoint("stopped MQTT adapters");

            // VT: NOTE: These can be done in parallel with MQTT

            logger.error("FIXME: deactivate 1-Wire");
            logger.error("FIXME: deactivate XBee");

        } catch (Exception ex) {
            apologize(ex, Level.FATAL);
        } finally {
            logger.warn("done");
            m.close();
            ThreadContext.pop();
        }
    }

    private void apologize(Throwable t) {
        apologize(t, Level.WARN);
    }

    private void apologize(Throwable t, Level level) {
        logger.log(level, "Unexpected exception, nothing we can do about it here", t);
    }
}
