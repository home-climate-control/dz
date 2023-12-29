package net.sf.dz3r.device.driver.command;

import net.sf.dz3r.device.driver.event.DriverNetworkErrorEvent;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Base class for all device driver commands.
 *
 * @param <T> Device driver type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class DriverCommand<T> {

    protected final Logger logger = LogManager.getLogger();

    /**
     * Unique ID to track request/response type commands in {@link DriverNetworkEvent}.
     */
    public final UUID messageId;

    /**
     * Sink to use to issue more commands if necessary.
     */
    protected final FluxSink<DriverCommand<T>> commandSink;

    protected DriverCommand(UUID messageId, FluxSink<DriverCommand<T>> commandSink) {
        this.messageId = messageId;
        this.commandSink = commandSink;
    }

    public final Flux<DriverNetworkEvent> execute(T adapter, DriverCommand<T> command) {
        return Flux.create(sink -> {
            try {

                execute(adapter, command, sink);
                sink.complete();

            } catch (Exception ex) {

                // This exception is likely to be mumbled over in subscribers, let's see if this is not too verbose
                logger.error("Error executing {}", command, ex);

                var flags = assessErrorFlags(ex);
                sink.next(new DriverNetworkErrorEvent<>(Instant.now(), command.messageId, flags.get(0), flags.get(1), ex));
            }
        });
    }

    protected abstract List<Boolean> assessErrorFlags(Exception ex);

    protected abstract void execute(T adapter, DriverCommand<T> command, FluxSink<DriverNetworkEvent> eventSink) throws Exception; // NOSONAR Good luck with that.

    @Override
    public String toString() {
        return "{" + getClass().getSimpleName() + " messageId=" + messageId + "}";
    }
}
