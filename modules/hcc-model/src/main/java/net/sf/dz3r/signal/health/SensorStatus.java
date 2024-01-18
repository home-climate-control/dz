package net.sf.dz3r.signal.health;

import java.time.Duration;
import java.util.Optional;

/**
 * Status of any {@link net.sf.dz3r.signal.Signal} emitter.
 *
 * Note that this object bears no failure indication, this is what {@link net.sf.dz3r.signal.Signal#status} is for.
 *
 * @param resolution Detected resolution.
 * @param stats Signal statistics.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public record SensorStatus(
        Double resolution,
        Optional<SignalStats<Duration>> stats
) {
}
