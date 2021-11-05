package net.sf.dz3.device.sensor;

/**
 * Provides ability to convert analog signal in arbitrarily complex way.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2010
 *
 * @deprecated Use {@code AnalogConverter} in {@code net.sf.dz3r.signal.filter}.
 */
@Deprecated
public interface AnalogConverter {

    /**
     * Convert input signal into output signal.
     *
     * @param signal Source signal to convert.
     *
     * @return Converted signal. If the source signal was {@code null}, output signal will
     * also be {@code null}.
     */
    public Double convert(Double signal);
}
