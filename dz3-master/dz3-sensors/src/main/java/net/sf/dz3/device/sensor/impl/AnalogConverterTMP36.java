package net.sf.dz3.device.sensor.impl;

import net.sf.dz3.device.sensor.AnalogConverter;

/**
 * Converts voltage in mV to temperature in °C for
 * {@link http://www.analog.com/en/temperature-sensing-and-thermal-management/digital-temperature-sensors/tmp36/products/product.html
 * TMP36} analog temperature sensor.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com"> Vadim Tkachenko 2010-2020
 *
 * @see AnalogConverterLM34
 */
public class AnalogConverterTMP36 implements AnalogConverter {

    /**
     * Convert voltage in mV to temperature in °C.
     *
     * TMP36 is expected to produce 750 mV at 25°C, and change linearly by 10mV/°C within
     * its operating range.
     *
     *  @param Input voltage, mV.
     *
     *  @return Temperature, °C.
     */
    @Override
    public Double convert(Double signal) {

        if (signal == null) {
            return null;
        }

        // 750mV @25°C, 10mV/°C
        return 25 - (750 - signal) / 10;
    }
}
