package net.sf.dz3r.device.actuator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Common functionality for all HVAC device drivers.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class AbstractHvacDevice implements HvacDevice {

    protected final Logger logger = LogManager.getLogger();

    private final String name;

    /**
     * The moment this device turned on, {@code null} if currently off.
     */
    private Instant startedAt;

    private boolean isClosed;

    protected AbstractHvacDevice(String name) {
        this.name = name;
    }

    @Override
    public final String getAddress() {
        return name;
    }

    protected void check(Switch<?> s, String purpose) {

        if (s == null) {
            throw new IllegalArgumentException("'" + purpose + "' switch can't be null");
        }
    }

    /**
     * Update uptime.
     *
     * @param state {@code true} if on.
     */
    protected final synchronized void updateUptime(boolean state) {

        if (state) {

            if (startedAt == null) {
                startedAt = Instant.now();
            }

        } else {

            startedAt = null;
        }
    }

    /**
     * Get the device uptime.
     *
     * @return For how long the device has been turned on, {@code null} if currently off.
     */
    public Duration uptime() {
        return startedAt == null ? null : Duration.between(startedAt, Instant.now());
    }

    protected boolean isClosed() {
        return isClosed;
    }

    @Override
    public final void close() throws IOException {

        if (isClosed) {
            logger.warn("redundant close(), ignored");
            return;
        }

        isClosed = true;
        doClose();
    }

    protected abstract void doClose() throws IOException;
}
