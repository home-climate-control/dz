package net.sf.dz3.device.actuator.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.model.Economizer;
import net.sf.dz3.device.model.ZoneController;
import net.sf.dz3.device.sensor.Switch;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;

/**
 * Extremely simplified implementation of a consumer for a {@link ZoneController}, {@link Economizer},
 * and other sources that might have a simple actuator.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public class SingleSwitchDevice implements DataSink<Double> {

    private final Logger logger = LogManager.getLogger(getClass());

    private final BlockingQueue<Runnable> commandQueue = new LinkedBlockingQueue<Runnable>();
    private final ThreadPoolExecutor executor;

    /**
     * Hardware switch to control.
     */
    private final Switch theSwitch;

    /**
     * Stays {@code true} until {@link #powerOff()} is called, then becomes {@code false}
     * and causes all subsequent invocations of {@link #consume(DataSample)} to throw
     * {@link IllegalStateException}.
     */
    private boolean enabled = true;

    /**
     * Current state.
     *
     * Used to avoid wasting time on setting hardware to the state it is already at.
     * {@code null} until first invocation.
     */
    private Boolean state;

    /**
     * Create an instance.
     *
     * @param source Data source to use to control {@link #theSwitch}. Non-zero output from the data source (or any other signal
     * passed to {@link #consume(DataSample)}, for that matter) will cause {@link #theSwitch} to be set to {@code true}.
     *
     * @param target Switch to control.
     */
    public SingleSwitchDevice(DataSource<Double> source, Switch target) {

        executor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, commandQueue);

        this.theSwitch = target;

        source.addConsumer(this);
    }

    @Override
    public synchronized void consume(DataSample<Double> signal) {

        ThreadContext.push("consume");

        try {

            checkEnabled();

            if (signal.isError()) {
                throw new IllegalStateException("Not accepting error input");
            }

            boolean newState = signal.sample > 0 ? true : false;

            if (state != null && newState == state.booleanValue()) {

                logger.info("Switch " + theSwitch.getAddress() + "=" + newState + " (unchanged)");
                return;
            }

            logger.info("Switch " + theSwitch.getAddress() + "=" + newState);

            executor.execute(new Command(newState));

        } finally {
            ThreadContext.pop();
        }

    }

    private void checkEnabled() {
        if (!enabled) {
            throw new IllegalStateException("powerOff() was called already");
        }
    }

    /**
     * Set {@link #theSwitch} to {@code false}.
     */
    public synchronized void powerOff() {

        ThreadContext.push("powerOff");

        try {

            logger.warn("Powering off");

            executor.execute(new Command(false));
            enabled = false;

            logger.info("shut down.");

        } finally {
            ThreadContext.pop();
        }
    }

    private class Command implements Runnable {

        private final boolean state;

        public Command(boolean state) {

            this.state = state;
        }

        @Override
        public void run() {

            int retry = 0;
            while (true) {

                ThreadContext.push("run" + (retry > 0 ? "#retry-" + retry : ""));

                try {

                    logger.debug("Running " + toString());

                    theSwitch.setState(state);
                    SingleSwitchDevice.this.state = state;

                    logger.debug("Success");
                    return;

                } catch (Throwable t) {

                    logger.fatal("Failed to execute " + getClass().getSimpleName(), t);

                    // We're going to retry this till the end of time, because
                    // hardware operations are critical

                    retry++;

                    try {

                        Thread.sleep(1000);

                    } catch (InterruptedException ex) {

                        logger.error("Interrupted, ignored", ex);
                    }

                } finally {
                    ThreadContext.pop();
                    ThreadContext.clearStack();
                }
            }
        }
    }
}
