package net.sf.dz3r.device.actuator;

import net.sf.dz3r.device.Addressable;
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
 * @param <T> Device status type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface HvacDevice<T> extends SignalProcessor<HvacCommand, HvacDeviceStatus<T>, Void>, Addressable<String>, AutoCloseable {

    /**
     * Find out which modes this device supports.
     *
     * @return Set of supported modes.
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
    Flux<Signal<HvacDeviceStatus<T>, Void>> compute(Flux<Signal<HvacCommand, Void>> in);

    /**
     * Get the stream of status updates.
     *
     * @return The mirror of the flux issued by {@link #compute(Flux)}.
     */
    Flux<Signal<HvacDeviceStatus<T>, Void>> getFlux();
}
