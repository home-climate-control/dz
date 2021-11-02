package net.sf.dz3r.device.xbee.command;

import com.homeclimatecontrol.xbee.AddressParser;
import com.homeclimatecontrol.xbee.XBeeReactive;
import com.homeclimatecontrol.xbee.zigbee.NetworkBrowser;
import com.rapplogic.xbee.api.zigbee.ZBNodeDiscover;
import net.sf.dz3r.device.driver.command.DriverCommand;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.device.xbee.event.XBeeNetworkArrival;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.FluxSink;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class XBeeCommandRescan extends XBeeCommand {

    public final Set<String> knownDevices;

    public XBeeCommandRescan(FluxSink<DriverCommand<XBeeReactive>> commandSink, Set<String> knownDevices) {
        super(UUID.randomUUID(), commandSink);
        this.knownDevices = knownDevices;
    }

    @Override
    protected void execute(XBeeReactive adapter, DriverCommand<XBeeReactive> command, FluxSink<DriverNetworkEvent> eventSink) throws Exception {
        ThreadContext.push("rescan");
        var m = new Marker("rescan");
        try {

            var scan = new NetworkBrowser().browse(adapter);

            logger.debug("ND timeout={}", scan.timeout);

            scan.discovered
                    .doOnNext(n -> checkArrival(knownDevices, n, eventSink))
                    .blockLast();



        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    private void checkArrival(Set<String> known, ZBNodeDiscover node, FluxSink<DriverNetworkEvent> eventSink) {

        var address = AddressParser.render4x4(node.getNodeAddress64());
        if (!known.contains(address)) {
            logger.warn("Arrived: {}", node);
            eventSink.next(new XBeeNetworkArrival(Instant.now(), address));
        }
    }
}
