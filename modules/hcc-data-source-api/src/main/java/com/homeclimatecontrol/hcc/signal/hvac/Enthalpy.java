package com.homeclimatecontrol.hcc.signal.hvac;

/**
 * An object that holds the value of enthalpy along with components it was obtained from.
 *
 * Humidity and pressure are nullable to indicate there are no sensors providing them.
 *
 * It is expected that the enthalpy value in this case is calculated based on
 * 50% relative humidity and standard atmospheric pressure of 1013.25 hPa.
 *
 * All components other than {@link #enthalpy} are provided for telemetry purposes only, and must not be used directly.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2026
 */
public record Enthalpy(
        double enthalpy,
        double temperature,
        Double humidity,
        Double pressure
) {

//     This is how the Enthalpy is expected to be created from just the temperature;
//     the code is commented out to avoid introducing project dependencies into a public API library

     /*
    public Enthalpy(double temperature, Double humidity, Double pressure) {
        this(
                PsychrometricsTool.calculateEnthalpy(
                        temperature,
                        humidity == null ? 0.5 : humidity,
                        pressure == null ? PsychrometricsTool.STANDARD_ATMOSPHERIC_PRESSURE : pressure),
                temperature,
                humidity,
                pressure);
    }
    */
}
