package com.homeclimatecontrol.hcc.hvac;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import static com.homeclimatecontrol.hcc.hvac.PsychrometricsTool.STANDARD_ATMOSPHERIC_PRESSURE;
import static com.homeclimatecontrol.hcc.hvac.PsychrometricsTool.calculateEnthalpy;
import static com.homeclimatecontrol.hcc.hvac.PsychrometricsTool.getAirSpecificHeat;

class PsychrometricsToolTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void printEnthalpyDiff() {

        // https://github.com/home-climate-control/dz/issues/348
        //
        // Good luck explaining to people what enthalpy is so that they can configure their economizer's
        // changeover-delta (which is already mysterious enough). Let's see how that translates into human readable terms.
        // Let's also print this as a CSV so that it can be imported and charted.

        // Based on these charts, it looks like doubling the changeover-delta will yield a reasonable approximation.

        printEnthalpyDiffByTemperature();
        printEnthalpyDiffByHumidity();
    }

    void printEnthalpyDiffByTemperature() {

        logger.info("temperature,enthalpy at 50% RH,delta");

        Double trailer = null;

        for (var t = 15; t <= 30; t++) {
            var e = calculateEnthalpy(t, 0.5, STANDARD_ATMOSPHERIC_PRESSURE);

            if (trailer != null) {
                logger.info("{},{},{}", t, e, e - trailer);
            }

            trailer = e;
        }
    }

    void printEnthalpyDiffByHumidity() {

        logger.info("humidity at 25°C,enthalpy,delta");

        Double trailer = null;

        for (var rh = 10; rh <= 100; rh += 5) {
            var e = calculateEnthalpy(25, rh / 100.0, STANDARD_ATMOSPHERIC_PRESSURE);

            if (trailer != null) {
                logger.info("{},{},{}", rh, e, e - trailer);
            }

            trailer = e;
        }
    }

    @Test
    void printAirSpecificHeat() {

        for (var t = 0; t < 100; t++) {
            logger.info("specific heat of air at {}°C: {}", t, getAirSpecificHeat(t));
        }
    }
}
