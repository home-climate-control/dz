package net.sf.dz3r.device.actuator;

import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import net.sf.dz3r.controller.SignalProcessor;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.HvacCommand;
import net.sf.dz3r.signal.HvacDeviceStatus;
import net.sf.dz3r.signal.Signal;
import reactor.core.publisher.Flux;

import java.util.Set;

public interface HvacDevice extends SignalProcessor<HvacCommand, HvacDeviceStatus, Void>, Addressable<String>, JmxAware {

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
    public Flux<Signal<HvacDeviceStatus, Void>> compute(Flux<Signal<HvacCommand, Void>> in);
}
