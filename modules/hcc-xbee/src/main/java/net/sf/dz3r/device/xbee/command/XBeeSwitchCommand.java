package net.sf.dz3r.device.xbee.command;

import com.homeclimatecontrol.xbee.XBeeReactive;
import net.sf.dz3r.device.driver.command.DriverCommand;
import reactor.core.publisher.FluxSink;

import java.util.UUID;

public abstract class XBeeSwitchCommand extends XBeeCommand {

    public final String address;

    /**
     * Create an instance without the command sink.
     *
     * @param address Switch address to send the command to.
     */
    protected XBeeSwitchCommand(UUID messageId, FluxSink<DriverCommand<XBeeReactive>> commandSink, String address) {
        super(messageId, commandSink);

        if (address == null) {
            throw new IllegalArgumentException("address can't be null");
        }

        this.address = address;
    }
}
