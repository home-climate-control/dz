package net.sf.dz3r.device.xbee.command;

import com.homeclimatecontrol.xbee.XBeeReactive;
import net.sf.dz3r.device.xbee.event.XBeeNetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.UUID;

/**
 * Base class for all XBee commands.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
public class XBeeCommand {

    protected final Logger logger = LogManager.getLogger();

    /**
     * Unique ID to track request/response type commands in {@link XBeeNetworkEvent}.
     */
    public final UUID messageId;

    /**
     * Sink to use to issue more commands if necessary.
     */
    protected final FluxSink<XBeeCommand> commandSink;

    public XBeeCommand(UUID messageId, FluxSink<XBeeCommand> commandSink) {
        this.messageId = messageId;
        this.commandSink = commandSink;
    }

    public final Flux<XBeeNetworkEvent> execute(XBeeReactive xbee, XBeeCommand command)  {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
