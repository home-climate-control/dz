package net.sf.dz3r.signal.filter;

/**
 * Converts voltage in mV to temperature in °C for
 * <a href="http://www.national.com/ds/LM/LM34.pdf">LM34</a> analog temperature sensor.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2010
 *
 * @see AnalogConverterTMP36
 */
public class AnalogConverterLM34 implements AnalogConverter {

    /**
     * Convert voltage in mV to temperature in °C (not °F!).
     *
     * LM34 is expected to produce 3000 mV at 300°F, -500 mV at -50°F, and change linearly
     * by 10mV/°F within its operating range.
     *
     *  @param signal Input voltage, mV.
     *
     *  @return Temperature, °C (not °F!).
     */
    @Override
    public Double convert(Double signal) {

        if (signal == null) {
            return null;
        }

        // 0mV @0°F, 10mV/°F
        // convert to Celsius
        return (((signal / 10) - 32) * 5) / 9d;
    }
}
