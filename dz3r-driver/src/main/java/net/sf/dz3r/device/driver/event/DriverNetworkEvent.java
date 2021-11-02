package net.sf.dz3r.device.driver.event;

import net.sf.dz3r.device.driver.command.DriverCommand;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all events that can be emitted by a device driver network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public class DriverNetworkEvent {

    public final Instant timestamp;

    /**
     * The {@link DriverCommand#messageId} of the command that resulted in this event.
     *
     * May be {@code null} if this event was not originated by a command, or if the command is unimportant.
     */
    public final UUID correlationId;

    public DriverNetworkEvent(Instant timestamp, UUID correlationId) {
        this.timestamp = timestamp;
        this.correlationId = correlationId;
    }
}
