package net.sf.dz3r.model;

import com.homeclimatecontrol.hcc.signal.hvac.HvacCommand;
import net.sf.dz3r.device.Addressable;
import net.sf.dz3r.signal.SignalProcessor;
import net.sf.dz3r.signal.hvac.UnitControlSignal;

/**
 * Accepts signals from {@link ZoneController}, issues signals to HVAC hardware and {@code DamperController}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface UnitController extends SignalProcessor<UnitControlSignal, HvacCommand, Void>, Addressable<String> {

}
