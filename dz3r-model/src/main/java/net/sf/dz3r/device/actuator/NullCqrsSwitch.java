package net.sf.dz3r.device.actuator;

import net.sf.dz3r.device.DeviceState;
import net.sf.dz3r.signal.Signal;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.Random;

/**
 * Does absolutely nothing other than fulfilling the API contract and reflecting itself in the log.
 *
 * A useful tool for troubleshooting a configuration without actual hardware available.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public class NullCqrsSwitch extends AbstractCqrsDevice<Boolean, Boolean> implements CqrsSwitch<String> {

    private static final Random rg = new SecureRandom();
    private boolean available = true;

    private final Duration minDelay;
    private final Duration maxDelay;

    public NullCqrsSwitch(String address) {
        this(
                address,
                Clock.systemUTC(),
                null,
                null,
                null,
                null);
    }

    public NullCqrsSwitch(
            String address,
            Clock clock,
            Duration heartbeat, Duration pace,
            Duration minDelay, Duration maxDelay) {

        super(address, clock, heartbeat, pace);

        this.minDelay = Optional.ofNullable(minDelay).orElse(Duration.ZERO);
        this.maxDelay = Optional.ofNullable(maxDelay).orElse(Duration.ZERO);

        checkDelays();
    }

    private void checkDelays() {

        if (minDelay.isNegative() || maxDelay.isNegative() || minDelay.minus(maxDelay).toMillis() > 0) {
            throw new IllegalArgumentException("invalid delays min=" + minDelay + ", max=" + maxDelay);
        }
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String getAddress() {
        return id;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public DeviceState<Boolean> getState() {
        return new DeviceState<>(id, available, requested, actual, 0);
    }

    @Override
    protected void setStateSync(Boolean command) {
        var delay = getDelay();
        logger.trace("{}: setStateSync={} wait({})...", getAddress(), command, delay);
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", ex);
        }
        logger.trace("{}: setStateSync={} done", getAddress(), command);
        this.actual = command;
    }

    private Duration getDelay() {
        if (minDelay.isZero() && maxDelay.isZero()) {
            return Duration.ZERO;
        }

        return minDelay.plus(Duration.ofMillis(rg.nextLong(maxDelay.toMillis())));
    }

    @Override
    public synchronized DeviceState<Boolean> setState(Boolean command) {

        logger.info("{}: setState={}", getAddress(), command);
        this.requested = command;

        queueDepth.incrementAndGet();
        commandSink.tryEmitNext(command);

        var state = getState();
        stateSink.tryEmitNext(new Signal<>(clock.instant(), state, id));

        return state;
    }

    @Override
    protected Boolean getCloseCommand() {
        return false;
    }

    @Override
    protected void closeSubclass() throws Exception {
        logger.info("{}: closeSubclass()", getAddress());
    }
}
