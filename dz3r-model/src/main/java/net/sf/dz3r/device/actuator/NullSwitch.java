package net.sf.dz3r.device.actuator;

import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.util.Optional;

/**
 * Does absolutely nothing other than reflecting itself in the log (and later via JMX).
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2021
 */
public class NullSwitch extends AbstractSwitch<String> {

    private Boolean state;

    protected NullSwitch(String address) {
        super(address);
    }

    protected NullSwitch(String address, Scheduler scheduler) {
        super(address, scheduler);
    }

    @Override
    protected void setStateSync(boolean state) throws IOException {
        logger.info("setState={}", state);
        this.state = state;
    }

    /**
     * Get the state.
     *
     * @return {@link #state}.
     * @throws IOException if {@link #setStateSync(boolean)} was not called yet and {@link #state} is {@code null}.
     */
    @Override
    protected boolean getStateSync() throws IOException {
        logger.info("getState={}", state);
        return Optional.ofNullable(state).orElseThrow(() -> new IOException("setStateSync() hasn't been called yet"));
    }
}
