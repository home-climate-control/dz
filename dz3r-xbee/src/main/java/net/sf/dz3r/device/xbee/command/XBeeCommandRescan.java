package net.sf.dz3r.device.xbee.command;

import reactor.core.publisher.FluxSink;

import java.util.Set;
import java.util.UUID;

public class XBeeCommandRescan extends XBeeCommand {

    public final Set<String> knownDevices;

    public XBeeCommandRescan(FluxSink<XBeeCommand> commandSink, Set<String> knownDevices) {
        super(UUID.randomUUID(), commandSink);
        this.knownDevices = knownDevices;
    }
}
