package net.sf.dz3r.device.driver;

import net.sf.dz3r.device.driver.command.DriverCommand;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Watches over a device driver network, handles events, executes commands.
 *
 * @param <T> Device adapter type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class DriverNetworkMonitor<T> {

    protected final Logger logger = LogManager.getLogger();

    protected final Duration rescanInterval;
    private final FluxSink<DriverNetworkEvent> observer;

    protected final Set<String> devicesPresent = Collections.synchronizedSet(new TreeSet<>());
    private FluxSink<DriverCommand<T>> commandSink;

    protected DriverNetworkMonitor(Duration rescanInterval, FluxSink<DriverNetworkEvent> observer) {
        this.rescanInterval = rescanInterval;
        this.observer = observer;
    }

    protected final void connect(FluxSink<DriverCommand<T>> commandSink) {
        this.commandSink = commandSink;
    }

    /**
     * Get the command sink.
     *
     * This method violates the Principle of Least Privilege (outsiders can do things with the sink they really shouldn't),
     * so let's make a mental note of replacing it with a complicated {@code submit()} that will shield the sink from
     * malicious (and dumb) programmers (it will still be useful for subclasses, though).
     *
     * @return {@link #commandSink}.
     */
    public final FluxSink<DriverCommand<T>> getCommandSink() {
        if (commandSink == null) {
            throw new IllegalStateException("commandSink still not initialized");
        }

        return commandSink;
    }

    protected abstract void handleEvent(DriverNetworkEvent event);

    protected final Flux<DriverNetworkEvent> execute(DriverCommand<T> command) {
        ThreadContext.push("execute");
        try {

            return command.execute(getAdapter(), command);

        } catch (Throwable t) {
            logger.error("Command execution failed: {}", command, t);

            // VT: FIXME: No response for now, but we'll need to propagate error status
            return Flux.empty();

        } finally {
            ThreadContext.pop();
        }
    }

    protected final void broadcastEvent(DriverNetworkEvent event) {
        observer.next(event);
    }

    protected abstract T getAdapter() throws IOException;
}
