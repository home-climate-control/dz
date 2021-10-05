package net.sf.dz3r.device.onewire.command;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import net.sf.dz3r.device.onewire.event.OneWireNetworkErrorEvent;
import net.sf.dz3r.device.onewire.event.OneWireNetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for all 1-Wire commands.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class OneWireCommand {
    protected final Logger logger = LogManager.getLogger();

    /**
     * Unique ID to track request/response type commands in {@link OneWireNetworkEvent}.
     */
    public final UUID messageId;

    /**
     * Sink to use to issue more commands if necessary.
     */
    protected final FluxSink<OneWireCommand> commandSink;

    protected OneWireCommand(UUID messageId, FluxSink<OneWireCommand> commandSink) {
        this.messageId = messageId;
        this.commandSink = commandSink;
    }

    public final Flux<OneWireNetworkEvent> execute(DSPortAdapter adapter, OneWireCommand command) {
        return Flux.create(sink -> {
            try {
                execute(adapter, command, sink);
            } catch (OneWireException ex) {
                var flags = assessErrorFlags(ex);
                sink.next(new OneWireNetworkErrorEvent<>(Instant.now(), command.messageId, flags.get(0), flags.get(1), ex));
            }
            sink.complete();
        });
    }

    protected List<Boolean> assessErrorFlags(OneWireException ex) {

        if (ex.getMessage() != null && ex.getMessage().equals("Error short on 1-Wire during putByte")) {
            return List.of(
                    true, // transient, can be corrected on the fly
                    false // not fatal, even though it kills the network, the network comes back after the short is gone
            );
        }

        var result = new ArrayList<Boolean>();

        result.add(null);
        result.add(null);

        return result;
    }

    protected abstract void execute(DSPortAdapter adapter, OneWireCommand command, FluxSink<OneWireNetworkEvent> eventSink) throws OneWireException;

    @Override
    public String toString() {
        return "{" + getClass().getSimpleName() + " messageId=" + messageId + "}";
    }
}
