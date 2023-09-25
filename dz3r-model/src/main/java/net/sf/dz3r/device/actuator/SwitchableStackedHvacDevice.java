package net.sf.dz3r.device.actuator;

import net.sf.dz3r.jmx.JmxDescriptor;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A stack of {@link SwitchableHvacDevice switchable HVAC devices}.
 *
 * Adds a multistage capability.
 *
 * @see net.sf.dz3r.model.MultistageUnitController
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class SwitchableStackedHvacDevice extends AbstractHvacDevice {

    private final HvacMode mode;
    private final List<SwitchableHvacDevice> devices;

    protected SwitchableStackedHvacDevice(String name, HvacMode mode, List<SwitchableHvacDevice> devices) {
        super(name);
        this.mode = mode;
        this.devices = verify(devices, mode);
    }

    /**
     * Verify the given stack and return it if it is OK.
     *
     * The same device can't be present in the list more than once.
     * Only devices that support the given mode can be in the list.
     *
     * @param devices Device list to verify.
     * @param mode Mode to comply with.
     *
     * @return The {@code devices} argument.
     * @throws IllegalArgumentException if the set can't be used.
     */
    private List<SwitchableHvacDevice> verify(List<SwitchableHvacDevice> devices, HvacMode mode) {

        var noncompliant = devices.stream().filter(d -> !d.getModes().contains(mode)).collect(Collectors.toSet());

        if (!noncompliant.isEmpty()) {
            throw new IllegalArgumentException("Wrong device mode (not " + mode + "): " + noncompliant);
        }

        var all = new HashSet<>(devices);
        var diff = devices.size() - all.size();

        if (diff > 0) {
            throw new IllegalArgumentException(diff + " duplicate devices in the list: " + devices);
        }

        return devices;
    }

    @Override
    public Set<HvacMode> getModes() {
        return Set.of(mode);
    }

    @Override
    public Flux<Signal<HvacDeviceStatus, Void>> compute(Flux<Signal<HvacCommand, Void>> in) {
        // VT: NOTE: Don't forget to setFlux() here
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public JmxDescriptor getJmxDescriptor() {
        return new JmxDescriptor(
                "dz",
                "Switchable Stacked HVAC Device",
                getAddress(),
                "Stack of switchable HVAC devices");
    }

    @Override
    protected void doClose() throws IOException {
        logger.warn("Shutting down: {}", getAddress());
        for (var s : devices) {
            logger.warn("close(): shutting down {} ", s);
            s.close();
        }
        logger.info("Shut down: {}", getAddress());
    }
}
