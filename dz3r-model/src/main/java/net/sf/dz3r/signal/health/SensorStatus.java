package net.sf.dz3r.signal.health;

import java.time.Duration;

/**
 * Status of any {@link net.sf.dz3r.signal.Signal} emitter.
 *
 * Note that this object bears no failure indication, this is what {@link net.sf.dz3r.signal.Signal#status} is for.
 *
 * @param resolution Detected resolution.
 * @param period Detected average period.
 * @param periodStDev Detected period standard deviation.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public record SensorStatus(
        Double resolution,
        Duration period,
        Duration periodStDev
) {
}
