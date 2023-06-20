package net.sf.dz3r.device.actuator;

import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.jmx.JmxAware;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.hvac.HvacCommand;
import net.sf.dz3r.signal.hvac.HvacDeviceStatus;
import reactor.core.publisher.Flux;

import java.util.Set;

/**
 * HVAC device driver.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public interface HvacDevice extends SignalProcessor<HvacCommand, HvacDeviceStatus, Void>, Addressable<String>, JmxAware, AutoCloseable {

    /**
     * Find out which modes this device supports.
     *
     * @return List of supported modes.
     */
    Set<HvacMode> getModes();

    /**
     * Execute the command.
     *
     * The side effect of this method (HVAC devices changing state) is actually the primary goal, emitted flux
     * is only for telemetry purposes.
     *
     * @param in Input flux.
     *
     * @return Execution status.
     */
    @Override
    Flux<Signal<HvacDeviceStatus, Void>> compute(Flux<Signal<HvacCommand, Void>> in);
}
