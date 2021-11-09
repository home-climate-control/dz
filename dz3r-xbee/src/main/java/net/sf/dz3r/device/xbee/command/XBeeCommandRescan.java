package net.sf.dz3r.device.xbee.command;

import com.homeclimatecontrol.xbee.AddressParser;
import com.homeclimatecontrol.xbee.XBeeReactive;
import com.homeclimatecontrol.xbee.response.command.NDResponse;
import com.homeclimatecontrol.xbee.zigbee.NetworkBrowser;
import net.sf.dz3r.device.driver.command.DriverCommand;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.device.xbee.event.XBeeNetworkArrival;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Command to rescan the XBee network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2009-2021
 */
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

            logger.debug("Devices already known: {}", knownDevices);

            var scan = new NetworkBrowser()
                    .browse(adapter)
                    .subscribeOn(Schedulers.boundedElastic())
                    .block();

            scan // NOSONAR False positive, no NPE here
                    .discovered
                    .log()
                    .doOnNext(node -> checkArrival(knownDevices, node, eventSink))
                    .blockLast();

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    private void checkArrival(Set<String> known, NDResponse node, FluxSink<DriverNetworkEvent> eventSink) {

        var address = AddressParser.render4x4(node.address64);
        if (!known.contains(address)) {
            logger.warn("Arrived: {}({}) {}", AddressParser.render4x4(node.address64), node.nodeIdentifier, node.deviceType); // NOSONAR Shut up already
            eventSink.next(new XBeeNetworkArrival(Instant.now(), address));
        }
    }
}
