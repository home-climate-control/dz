package net.sf.dz3r.device.actuator;

import com.homeclimatecontrol.hcc.model.EconomizerSettings;
import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.ZoneStatus;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.model.Zone;
import net.sf.dz3r.signal.SignalProcessor;

/**
 * Economizer abstraction.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2026
 */
public interface Economizer extends SignalProcessor<Double, Double, String>, Addressable<String>, AutoCloseable {

    void setSettings(EconomizerSettings settings);

    /**
     * Figure out whether the HVAC needs to be suppressed and adjust the signal if so.
     *
     * @param source Signal computed by {@link Zone}.
     * @return Signal with {@link ZoneStatus#callingStatus()} possibly adjusted to shut off the HVAC if the economizer is active.
     */
    Signal<ZoneStatus, String> computeHvacSuppression(Signal<ZoneStatus, String> source);
}
