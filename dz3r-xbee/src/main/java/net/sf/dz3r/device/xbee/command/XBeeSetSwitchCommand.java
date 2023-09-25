package net.sf.dz3r.device.xbee.command;

import com.homeclimatecontrol.xbee.AddressParser;
import com.homeclimatecontrol.xbee.XBeeReactive;
import com.homeclimatecontrol.xbee.response.frame.RemoteATCommandResponse;
import com.rapplogic.xbee.api.AtCommand;
import com.rapplogic.xbee.api.RemoteAtRequest;
import net.sf.dz3r.common.StringChannelAddress;
import net.sf.dz3r.device.driver.command.DriverCommand;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.device.xbee.event.XBeeSwitchState;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class XBeeSetSwitchCommand extends XBeeSwitchCommand {

    public final boolean state;

    /**
     * Create an instance.
     *
     * @param address Switch address to send the command to.
     * @param state State to set.
     */
    public XBeeSetSwitchCommand(UUID messageId, FluxSink<DriverCommand<XBeeReactive>> commandSink, String address, boolean state) {
        super(messageId, commandSink, address);
        this.state = state;
    }

    @Override
    protected void execute(XBeeReactive adapter, DriverCommand<XBeeReactive> command, FluxSink<DriverNetworkEvent> eventSink) throws Exception {

        var marker = "setState(" + address + ")=" + state;
        ThreadContext.push(marker);
        var m = new Marker(marker);
        try {

            var channelAddress = new StringChannelAddress(address);
            var channel = channelAddress.channel;
            int deviceState = state ? 5 : 4;

            var response = adapter.sendAT(
                            new RemoteAtRequest(
                                    AddressParser.parse(channelAddress.hardwareAddress),
                                    AtCommand.Command.valueOf(channel),
                                    new int[] {deviceState}), Duration.ofSeconds(5))
                    .block();

            if (RemoteATCommandResponse.Status.OK == response.status) { // NOSONAR Acceptable
                // OK status is good enough, it was returned by the remote
                eventSink.next(new XBeeSwitchState(Instant.now(), command.messageId, address, state));
                return;
            }

            throw new IOException("Failed to set " + address + " to " + state + ", response: " + response);

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }
}
