package net.sf.dz3r.signal.health;

/**
 * Signal statistics.
 *
 * @param <U> Unit.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public record SignalStats<U>(
    U average,
    U sdDev) {
}
