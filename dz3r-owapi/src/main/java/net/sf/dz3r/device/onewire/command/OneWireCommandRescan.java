package net.sf.dz3r.device.onewire.command;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.utils.OWPath;
import net.sf.dz3r.device.onewire.event.OneWireNetworkArrival;
import net.sf.dz3r.device.onewire.event.OneWireNetworkDeparture;
import net.sf.dz3r.device.onewire.event.OneWireNetworkEvent;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.FluxSink;

import java.time.Instant;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Command to rescan the 1-Wire network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class OneWireCommandRescan extends OneWireCommand {
    public final Set<String> knownDevices;

    public OneWireCommandRescan(FluxSink<OneWireCommand> commandSink, Set<String> knownDevices) {
        super(commandSink);
        this.knownDevices = knownDevices;
    }

    @Override
    protected void execute(DSPortAdapter adapter, OneWireCommand command, FluxSink<OneWireNetworkEvent> eventSink) throws OneWireException {
        ThreadContext.push("rescan");
        var m = new Marker("rescan");
        try {

            var address2device = new TreeMap<String, OneWireContainer>();
            var address2path = new TreeMap<String, OWPath>();
            var known = new TreeSet<>(knownDevices);

            rescan(adapter, eventSink, new OWPath(adapter), address2device, address2path, known);

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    private void rescan(DSPortAdapter adapter, FluxSink<OneWireNetworkEvent> eventSink,
                        OWPath path,
                        TreeMap<String, OneWireContainer> address2device,
                        TreeMap<String, OWPath> address2path,
                        TreeSet<String> known) throws OneWireException {

        ThreadContext.push("rescan(" + path + ")");
        var m = new Marker("rescan(" + path + ")");
        try {

            adapter.setSearchAllDevices();
            adapter.targetAllFamilies();

            closeAllPaths(adapter);
            path.open();

            for (var owc : adapter.getAllDeviceContainers()) {

                String address = owc.getAddressAsString();
                logger.debug("Found: {} {} at {}",owc.getName(), address, path);
                address2device.put(address, owc);
                address2path.put(address, path);

                if (!known.contains(address)) {
                    logger.warn("Arrived: {}", owc);
                    eventSink.next(new OneWireNetworkArrival(Instant.now(), address));
                }

                known.remove(address);
            }

            if (!known.isEmpty()) {
                for (var address : known) {
                    logger.warn("Departed: {}", address);
                    eventSink.next(new OneWireNetworkDeparture(Instant.now(), address));
                }
            }

            // If new devices have arrived, it would be a good idea to poll them now

            if (commandSink == null) {
                logger.debug("commandSink is not connected yet");
                return;
            }

            commandSink.next(new OneWireCommandReadTemperatureAll(commandSink, address2device.keySet()));

        } finally {
            m.close();
            ThreadContext.pop();
        }

    }
}
