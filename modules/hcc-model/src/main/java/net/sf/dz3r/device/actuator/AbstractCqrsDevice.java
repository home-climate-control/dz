package net.sf.dz3r.device.actuator;

import com.homeclimatecontrol.hcc.device.DeviceState;
import com.homeclimatecontrol.hcc.signal.Signal;
import net.sf.dz3r.common.HCCObjects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractCqrsDevice<I, O> implements CqrsDevice<I, O> {

    protected final Logger logger = LogManager.getLogger();

    protected final String id;
    protected final Clock clock;


    /**
     *  Issue identical control commands to this device at least this often, repeat if necessary.
     */
    protected final Duration heartbeat;

    /**
     * Issue identical control commands to this device at most this often.
     */
    protected final Duration pace;

    protected final AtomicInteger queueDepth = new AtomicInteger();

    protected final Sinks.Many<I> commandSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Disposable commandSubscription;
    protected final Sinks.Many<Signal<DeviceState<O>, String>> stateSink = Sinks.many().multicast().onBackpressureBuffer();

    private I lastCommand;
    private Instant lastSet;

    protected O requested;
    protected O actual;

    protected AbstractCqrsDevice(String id, Clock clock, Duration heartbeat, Duration pace) {

        this.id = HCCObjects.requireNonNull(id, "id can't be null");
        this.clock = HCCObjects.requireNonNull(clock, "clock can't be null");

        // These are nullable
        this.heartbeat = heartbeat;
        this.pace = pace;

        commandSubscription = commandSink
                .asFlux()
                .publishOn(Schedulers.newSingle("cqrs-" + id))
                .flatMap(this::limitRate)
                .subscribe(this::setStateSync);
    }

    /**
     * Make sure {@link #setStateSync(Object)} is not called for identical commands more often than {@link #pace} interval in between.
     *
     * @param command Command to inspect
     *
     * @return Flux of just the input command if the pace is not exceeded, or empty Flux if it is.
     */
    synchronized Flux<I> limitRate(I command) {

        ThreadContext.push("limitRate");

        try {

            if (pace == null) {
                logger.trace("{}: null pace - passthrough command={}", id, command);
                return Flux.just(command);
            }

            var now = clock.instant();

            if (!command.equals(lastCommand)) {

                logger.trace("{}: command={} pass - different from {}", id, command, lastCommand);

                lastCommand = command;
                lastSet = now;

                return Flux.just(command);
            }

            var interval = Duration.between(lastSet, now);
            if (interval.compareTo(pace) < 0) {
                logger.trace("{}: command={} drop - too soon ({} vs {})", id, command, interval, pace);

                queueDepth.decrementAndGet();
                return Flux.empty();
            }

            logger.trace("{}: command={} pass - {} is beyond {}", id, command, interval, pace);
            lastSet = now;
            return Flux.just(command);

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public DeviceState<O> getState() {

        return new DeviceState<>(
                id,
                isAvailable(),
                requested,
                actual,
                queueDepth.get()
        );
    }

    protected Signal<DeviceState<O>, String> getStateSignal() {
        return new Signal<>(clock.instant(), getState(), id);
    }

    /**
     * Set the requested state, synchronously
     *
     * @param command Command to execute.
     */
    protected abstract void setStateSync(I command);

    @Override
    public final Flux<Signal<DeviceState<O>, String>> getFlux() {
        return stateSink.asFlux();
    }

    protected abstract I getCloseCommand();
    protected abstract void closeSubclass() throws Exception;

    @Override
    public final void close() throws Exception {

        // Prevent new commands from coming in
        commandSubscription.dispose();

        // Shut down the device
        setStateSync(getCloseCommand());

        // Adjust the queue depth - previous command skewed it
        queueDepth.incrementAndGet();

        // Emit the final notification
        stateSink.tryEmitNext(getStateSignal());

        // Indicate that we're done
        stateSink.tryEmitComplete();

        closeSubclass();
    }
}
