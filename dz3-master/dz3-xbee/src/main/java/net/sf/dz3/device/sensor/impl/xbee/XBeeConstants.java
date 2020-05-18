package net.sf.dz3.device.sensor.impl.xbee;

// VT: NOTE: Too much hassle for this use case.
@SuppressWarnings("squid:S1214")
public interface XBeeConstants {

    int TIMEOUT_AP_MILLIS = 5000;
    int TIMEOUT_AT_MILLIS = 5000;
    int TIMEOUT_IS_MILLIS = 5000;
    int TIMEOUT_NT_MILLIS = 5000;

    /**
     * XBee ZB hardware ADC resolution.
     *
     * 0 corresponds to 0V, 0x3FF corresponds to 1.2V.
     */
    double ADC_RESOLUTION = 1024d / 1200d;
}
