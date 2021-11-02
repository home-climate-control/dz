package net.sf.dz3r.device.onewire.command;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.OneWireContainer1F;
import com.dalsemi.onewire.container.SwitchContainer;
import com.dalsemi.onewire.utils.OWPath;
import net.sf.dz3r.device.driver.command.DriverCommand;
import net.sf.dz3r.device.driver.event.DriverNetworkEvent;
import net.sf.dz3r.device.onewire.event.OneWireNetworkArrival;
import net.sf.dz3r.device.onewire.event.OneWireNetworkDeparture;
import net.sf.dz3r.instrumentation.Marker;
import org.apache.logging.log4j.ThreadContext;
import reactor.core.publisher.FluxSink;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Command to rescan the 1-Wire network.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class OneWireCommandRescan extends OneWireCommand {

    public final Set<String> knownDevices;

    public OneWireCommandRescan(FluxSink<DriverCommand<DSPortAdapter>> commandSink, Set<String> knownDevices) {
        super(UUID.randomUUID(), commandSink);
        this.knownDevices = knownDevices;
    }

    @Override
    protected void execute(DSPortAdapter adapter, DriverCommand<DSPortAdapter> command, FluxSink<DriverNetworkEvent> eventSink) throws Exception {
        ThreadContext.push("rescan");
        var m = new Marker("rescan");
        try {

            var address2device = new TreeMap<String, OneWireContainer>();
            var address2path = new TreeMap<String, OWPath>();
            var known = new TreeSet<>(knownDevices);

            rescan(adapter, eventSink, new OWPath(adapter), address2device, address2path, known, 0);

        } finally {
            m.close();
            ThreadContext.pop();
        }
    }

    private void rescan(DSPortAdapter adapter, FluxSink<DriverNetworkEvent> eventSink,
                        OWPath path,
                        TreeMap<String, OneWireContainer> address2device,
                        TreeMap<String, OWPath> address2path,
                        TreeSet<String> known,
                        int depth) throws OneWireException {

        var logMarker = "rescan^" + depth + "(" + path + ")";
        ThreadContext.push(logMarker);
        var m = new Marker(logMarker);
        try {

            adapter.setSearchAllDevices();
            adapter.targetAllFamilies();

            adapter.closeAllPaths();
            path.open();

            var branches = new ArrayList<OWPath>();

            for (var owc : adapter.getAllDeviceContainers()) {

                String address = owc.getAddressAsString();

                if (address2device.containsKey(address)) {
                    // We've already handled this device
                    logger.debug("Already saw: {} {} at {}", owc.getName(), address, address2path.get(address));
                    continue;
                }

                logger.debug("Found: {} {} at {}", owc.getName(), address, path);

                address2device.put(address, owc);
                address2path.put(address, path);

                checkArrival(known, address, owc, path, eventSink);
                checkLanCoupler(owc, path, branches);

                known.remove(address);
            }

            logger.info("Branches found: {}", branches.size());
            branches.forEach(b -> logger.info("  {}", b));

            for (var branchPath : branches) {
                rescan(adapter, eventSink, branchPath, address2device, address2path, known, depth++);
            }

            if (depth == 0) {

                // We're done with all the branches

                checkDepartures(known, eventSink);

                // If new devices have arrived, it would be a good idea to poll them now

                if (commandSink == null) {
                    logger.debug("commandSink is not connected yet");
                    return;
                }

                commandSink.next(new OneWireCommandReadTemperatureAll(commandSink, address2device.keySet(), new TreeMap<>(address2path)));
            }

        } finally {
            m.close();
            ThreadContext.pop();
        }

    }

    private void checkArrival(TreeSet<String> known, String address, OneWireContainer owc, OWPath path, FluxSink<DriverNetworkEvent> eventSink) {
        if (!known.contains(address)) {
            logger.warn("Arrived: {}", owc);
            eventSink.next(new OneWireNetworkArrival(Instant.now(), address, path));
        }
    }

    private void checkDepartures(TreeSet<String> known, FluxSink<DriverNetworkEvent> eventSink) {
        if (!known.isEmpty()) {
            for (var address : known) {
                logger.warn("Departed: {}", address);
                eventSink.next(new OneWireNetworkDeparture(Instant.now(), address));
            }
        }
    }

    private void checkLanCoupler(OneWireContainer owc, OWPath path, ArrayList<OWPath> branches) {

        if (!(owc instanceof OneWireContainer1F)) {
            return;
        }

        var coupler = (SwitchContainer) owc;

        branches.add(path.extend(coupler, 0));
        branches.add(path.extend(coupler, 1));
    }
}
