package net.sf.dz3.device.sensor.impl.xbee;

/**
 * Conversion utilities for XBee and related hardware.
 *  
 * @author Copyright &copy; <a href="mailto:vt@seriousinsights.com">Vadim Tkachenko</a> 2010
 */
public class Converter {

    /**
     * XBee ZB hardware ADC resolution.
     * 
     * 0 corresponds to 0V, 0x3FF corresponds to 1.2V.
     */
    final static double resolution = 1024d / 1200d;

    /**
     * Convert the sensor reading returned by IS command to temperature,
     * assuming TMP36 sensor is connected.
     * 
     * @param sensorReading Raw sensor reading.
     * @return Temperature, in °C.
     */
    public static double ADC2C_TMP36(int sensorReading) {
        
        // 750mV @25°C, 10mV/°C
        return 25 - (750 - raw2mV(sensorReading)) / 10;
    }
    
    /**
     * Convert the raw ADC value returned into a value in mV.
     * 
     * @param sensorReading raw ADC reading.
     * @return Power supply voltage in mV.
     */
    public static double raw2mV(int sensorReading) {
        
        return sensorReading / resolution;
    }
}
