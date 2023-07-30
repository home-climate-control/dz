package net.sf.dz3r.signal.health;

import java.time.Duration;

/**
 * Status of any {@link net.sf.dz3r.device.actuator.Switch}
 *
 * Note that this object bears no failure indication, this is what {@link net.sf.dz3r.signal.Signal#status} is for.
 *
 * @param lag Detected average lag between requested and actual states.
 * @param lagStDev Detected lag standard deviation.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public record SwitchStatus(
        Duration lag,
        Duration lagStDev
) {
}
