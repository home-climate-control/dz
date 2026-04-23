package com.homeclimatecontrol.hcc.hvac;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import static com.homeclimatecontrol.hcc.hvac.PsychrometricsTool.STANDARD_ATMOSPHERIC_PRESSURE;

class PsychrometricsToolTest {

    private final Logger logger = LogManager.getLogger();

    @Test
    void printEnthalpy() {

        var temperature = 25;

        for (var rh = 10; rh <= 100; rh += 10) {
            logger.info("Enthalpy at temp={}°C rh={}%: {}",
                    temperature,
                    rh,
                    PsychrometricsTool.calculateEnthalpy(25, rh / 100.0, STANDARD_ATMOSPHERIC_PRESSURE));
        }
    }

    @Test
    void printAirSpecificHeat() {

        for (var t = 0; t < 100; t++) {
            logger.info("specific heat of air at {}°C: {}", t, PsychrometricsTool.getAirSpecificHeat(t));
        }
    }
}
