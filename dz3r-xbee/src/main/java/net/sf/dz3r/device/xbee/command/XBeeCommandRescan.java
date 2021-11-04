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

            var scan = new NetworkBrowser()
                    .browse(adapter)
                    .subscribeOn(Schedulers.boundedElastic())

                    // This block() covers sending NT command and receiving the response from local hardware, better not be interrupted
                    .block();

            // ...but this needs to be done elsewhere, the responses are coming back asynchronously from the XBee network
            // and with quite a generous timeout, no sense waiting
            new Thread(() -> {
                try {
                    scan
                            .discovered
                            .doOnNext(node -> checkArrival(knownDevices, node, eventSink))
                            .blockLast();
                } catch (Throwable t) { // NOSONAR This is intended, there's nobody else to report problems above us
                    logger.error("Failed to collect ND responses", t);
                }
            }).start();

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    private void checkArrival(Set<String> known, ZBNodeDiscover node, FluxSink<DriverNetworkEvent> eventSink) {

        var address = AddressParser.render4x4(node.getNodeAddress64());
        if (!known.contains(address)) {
            logger.warn("Arrived: {}({}) {}", AddressParser.render4x4(node.getNodeAddress64()), node.getNodeIdentifier(), node.getType()); // NOSONAR Shut up already
            eventSink.next(new XBeeNetworkArrival(Instant.now(), address));
        }
    }
}
