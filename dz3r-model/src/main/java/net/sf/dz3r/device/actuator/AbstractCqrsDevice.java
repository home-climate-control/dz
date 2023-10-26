package net.sf.dz3r.device.actuator;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.device.DeviceState;
import net.sf.dz3r.signal.Signal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
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
                .subscribe(this::setStateSync);
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
