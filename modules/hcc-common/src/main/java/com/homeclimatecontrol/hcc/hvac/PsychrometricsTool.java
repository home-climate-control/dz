package com.homeclimatecontrol.hcc.hvac;

/**
 * Psychrometrics related utilities.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2026
 */
public class PsychrometricsTool {

    /**
     * Standard atmospheric pressure in hPa.
     */
    public static final double STANDARD_ATMOSPHERIC_PRESSURE = 1013.25;

    /**
     * Calculate the enthalpy of moist air.
     *
     * @param temperature Temperature in °C.
     * @param relativeHumidity Relative humidity as a ratio (e.g., 0.5 for 50%).
     *
     * @return Enthalpy in kJ/kg of dry air.
     */
    public static double calculateEnthalpy(double temperature, double relativeHumidity, double atmosphericPressure) {

        if (relativeHumidity < 0 || relativeHumidity > 1) {
            throw new IllegalArgumentException("Relative humidity must be between 0 and 1, given value is " + relativeHumidity);
        }

        // 1. Calculate Saturation Vapor Pressure (P_ws) in hPa using Magnus-Tetens
        var saturatedPressure = 6.112 * Math.pow(10, (7.5 * temperature) / (temperature + 237.3));

        // 2. Calculate Actual Vapor Pressure (P_w)
        var actualVaporPressure = relativeHumidity * saturatedPressure;

        // 3. Calculate Humidity Ratio (omega)
        var humidityRatio = 0.622 * actualVaporPressure / (atmosphericPressure - actualVaporPressure);

        // 4. Calculate Enthalpy (h) in kJ/kg
        // h = (Specific Heat Air * T) + omega * (Latent Heat + Specific Heat Vapor * T)

        return (getAirSpecificHeat(temperature) * temperature) + humidityRatio * (2501 + 1.86 * temperature);
    }

    /**
     * Calculate the specific heat capacity (cp) of dry air at constant pressure.
     *
     * @param temperature Temperature in °C
     *
     * @return Specific heat capacity in kJ/(kg·K)
     */
    public static double getAirSpecificHeat(double temperature) {

        // Convert Celsius to Kelvin
        double T = temperature + 273.15;

        // Coefficients (Standard engineering polynomial for dry air)
        double a = 1.04841;
        double b = -3.83719e-4;
        double c = 9.45378e-7;
        double d = -5.49031e-10;
        double e = 7.92981e-14;

        // Horner's Method evaluation: a + T(b + T(c + T(d + Te)))
        return a + T * (b + T * (c + T * (d + T * e)));
    }
}
