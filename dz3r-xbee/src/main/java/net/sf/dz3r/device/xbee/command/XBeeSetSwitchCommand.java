package net.sf.dz3r.device.xbee.command;

import com.homeclimatecontrol.xbee.XBeeReactive;
import net.sf.dz3r.device.driver.command.DriverCommand;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.FluxSink;

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

            throw new UnsupportedOperationException("Not Implemented");

//            var channelAddress = new IntegerChannelAddress(address);
//            var device = adapter.getDeviceContainer(channelAddress.hardwareAddress);
//            adapter.closeAllPaths();
//            getPath(channelAddress.hardwareAddress, path).open();
//
//            m.checkpoint("path open: " + path);
//
//            if (!(device instanceof SwitchContainer)) {
//                throw new IllegalArgumentException(address + ": (" + device.getName() + ") is not a SwitchContainer");
//            }
//
//            var sc = (SwitchContainer) device;
//
//            var writtenState= writeDevice(m, channelAddress, sc, state);
//            var readState = readDevice(m, channelAddress, sc, new State(writtenState));
//
//            eventSink.next(new OneWireSwitchState(Instant.now(), command.messageId, address, readState.state[channelAddress.channel]));

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

//    private State writeDevice(Marker m, IntegerChannelAddress channelAddress, SwitchContainer sc, boolean state) throws OneWireException {
//
//        var rawState = sc.readDevice();
//
//        m.checkpoint("readDevice/0");
//
//        var channelCount = sc.getNumberChannels(rawState);
//
//        var deviceState = new State(channelCount, sc.hasSmartOn());
//        for (var offset = 0; offset < channelCount; offset++) {
//            deviceState.state[offset] = sc.getLatchState(offset, rawState);
//        }
//
//        logger.debug("readDevice/0 {}: {}", channelAddress.hardwareAddress, deviceState);
//
//        if (channelAddress.channel > deviceState.channels) {
//            throw new IllegalArgumentException(address + ": channel number is higher than supported number (" + deviceState.channels + ")");
//        }
//
//        sc.setLatchState(channelAddress.channel, state, deviceState.smart, rawState);
//        deviceState.state[channelAddress.channel] = state;
//
//        sc.writeDevice(rawState);
//        m.checkpoint("writeDevice/0");
//
//        return deviceState;
//    }
//
//    private State readDevice(Marker m, IntegerChannelAddress channelAddress, SwitchContainer sc, State deviceState) throws OneWireException {
//
//        var rawState = sc.readDevice();
//        m.checkpoint("readDevice/1");
//
//        for (var offset = 0; offset < deviceState.channels; offset++) {
//            deviceState.state[offset] = sc.getLatchState(offset, rawState);
//        }
//
//        logger.debug("readDevice/1 {}: {}", channelAddress.hardwareAddress, deviceState);
//
//        return deviceState;
//    }


    /**
     * Volatile switch state representation.
     */
    static class State {

        public final int channels;

        /**
         * True if the switch supports smart operation.
         */
        public boolean smart;

        /**
         * Switch state.
         */
        public final boolean[] state;

        public State(int channels, boolean smart) {
            this.channels = channels;
            this.state = new boolean[channels];
            this.smart = smart;
        }

        public State(State template) {

            this(template.channels, template.smart);
            System.arraycopy(template.state, 0, state, 0, channels);
        }

        /**
         * @return String representation of the switch state.
         */
        @Override
        public final String toString() {

            var sb = new StringBuilder("[");
            sb.append((smart ? "smart" : "dumb"));

            sb.append("][");

            for (var offset = 0; offset < state.length; offset++) {

                if (offset != 0) {
                    sb.append(",");
                }

                sb.append(state[offset]);
            }

            sb.append("]");

            return sb.toString();
        }
    }
}
