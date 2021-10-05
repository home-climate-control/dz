package net.sf.dz3r.device.onewire.event;

import net.sf.dz3r.device.onewire.command.OneWireCommand;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all events that can be emitted by a 1-Wire network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class OneWireNetworkEvent {

    public final Instant timestamp;

    /**
     * The {@link OneWireCommand#messageId} of the command that resulted in this event.
     *
     * May be {@code null} if this event was not originated by a command, or if the command is unimportant.
     */
    public final UUID correlationId;

    public OneWireNetworkEvent(Instant timestamp, UUID correlationId) {
        this.timestamp = timestamp;
        this.correlationId = correlationId;
    }
}
