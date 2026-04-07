package net.sf.dz3r.device.actuator.economizer;

import com.homeclimatecontrol.hcc.model.EconomizerSettings;
import com.homeclimatecontrol.hcc.model.HvacMode;
import com.homeclimatecontrol.hcc.signal.Signal;
import com.homeclimatecontrol.hcc.signal.hvac.CallingStatus;
import com.homeclimatecontrol.hcc.signal.hvac.ZoneStatus;
import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.device.actuator.NullCqrsSwitch;
import net.sf.dz3r.device.actuator.SwitchableHvacDevice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class AbstractEconomizerTest {

    private final Logger logger = LogManager.getLogger();

    /**
     * Make sure that control signal is computed properly in cooling mode as the indoor temperature is approaching the {@link EconomizerSettings#targetTemperature()}.
     */
    @ParameterizedTest
    @MethodSource("targetAdjustmentCoolingProvider")
    void targetAdjustmentCoolingTest(TargetAdjustmentTestData source) {

        var config = new EconomizerConfig(
                source.mode,
                1.0, 0.0001, 1.0,
                new EconomizerSettings(
                        source.changeoverDelta,
                        source.targetTemperature,
                        null,
                        1.0
                )
        );

        var e = new TestEconomizer(
                "eco-cool",
                config,
                new SwitchableHvacDevice(
                        Clock.systemUTC(),
                        "cooler",
                        HvacMode.COOLING,
                        new NullCqrsSwitch("s"),
                        false,
                        null)
        );

        testAdjustment(source, e);
    }

    /**
     * Make sure that control signal is computed properly in heating mode as the indoor temperature is approaching the {@link EconomizerSettings#targetTemperature()}.
     */
    @ParameterizedTest
    @MethodSource("targetAdjustmentHeatingProvider")
    void targetAdjustmentHeatingTest(TargetAdjustmentTestData source) {

        var config = new EconomizerConfig(
                source.mode,
                -1.0, -0.0001, 1.0,
                new EconomizerSettings(
                        source.changeoverDelta,
                        source.targetTemperature,
                        null,
                        1.0
                )
        );

        var e = new TestEconomizer(
                "eco-heat",
                config,
                new SwitchableHvacDevice(
                        Clock.systemUTC(),
                        "heater",
                        HvacMode.HEATING,
                        new NullCqrsSwitch("s"),
                        false,
                        null)
        );

        testAdjustment(source, e);
    }

    private void testAdjustment(TargetAdjustmentTestData source, TestEconomizer e) {

        var signal = e.computeCombined(source.indoorTemperature, source.ambientTemperature);

        assertThat(signal).isEqualTo(source.expectedSignal);
    }

    /**
     * Verify that when the economizer is active and HVAC demand is below the handoff factor, HVAC is suppressed.
     *
     * Scenario: ambient conditions favour economizer, zone demands HVAC, but demand is below threshold.
     * Expected: economizer on, HVAC off.
     */
    @ParameterizedTest
    @MethodSource("hvacSuppressionBelowFactorProvider")
    void hvacSuppressedBelowHandoffFactor(HvacSuppressionTestData source) {

        var config = new EconomizerConfig(
                HvacMode.COOLING,
                1.0, 0.0001, 1.0,
                new EconomizerSettings(2.0, 20.0, source.hvacHandoffFactor, 1.0)
        );

        var e = new TestEconomizer(
                "eco-suppress",
                config,
                new SwitchableHvacDevice(
                        Clock.systemUTC(),
                        "cooler",
                        HvacMode.COOLING,
                        new NullCqrsSwitch("s"),
                        false,
                        null)
        );

        // Simulate economizer being active
        e.setActuatorState(true);

        var zoneStatus = new ZoneStatus(
                new com.homeclimatecontrol.hcc.model.ZoneSettings(25.0),
                new CallingStatus(null, source.hvacDemand, true),
                null,
                null);

        var input = new Signal<>(Instant.now(), zoneStatus, (String) null);
        var result = e.computeHvacSuppression(input);

        assertThat(result.getValue().callingStatus().calling()).isEqualTo(source.expectHvacCalling);
    }

    private static Stream<HvacSuppressionTestData> hvacSuppressionBelowFactorProvider() {

        return Stream.of(
                // demand below factor: HVAC suppressed
                new HvacSuppressionTestData(1.5, 0.8, false),
                // demand equals factor: HVAC not suppressed
                new HvacSuppressionTestData(1.5, 1.5, true),
                // demand above factor: HVAC not suppressed
                new HvacSuppressionTestData(1.5, 2.0, true),
                // null factor (default MAX_VALUE): HVAC always suppressed
                new HvacSuppressionTestData(null, 100.0, false)
        );
    }

    private record HvacSuppressionTestData(
            Double hvacHandoffFactor,
            double hvacDemand,
            boolean expectHvacCalling) {
    }

    private static class TestEconomizer extends AbstractEconomizer {

        /**
         * Create an instance.
         * <p>
         * Note that only the {@code ambientFlux} argument is present, indoor flux is provided to {@link #compute(Flux)}.
         *
         * @param device HVAC device acting as the economizer.
         */
        protected TestEconomizer(String name, EconomizerConfig settings, HvacDevice device) {
            super(Clock.systemUTC(), name, settings, device, Duration.ofSeconds(90));
        }

        @Override
        protected Flux<Signal<Boolean, ProcessController.Status<Double>>> computeDeviceState(Flux<Signal<Double, Void>> signal) {
            throw new IllegalStateException("we don't need this");
        }

        @Override
        public double computeCombined(Double indoorTemperature, Double ambientTemperature) {

            // Just expose the protected super method for testing
            return super.computeCombined(indoorTemperature, ambientTemperature);
        }
    }

    private record TargetAdjustmentTestData(
            HvacMode mode,
            double changeoverDelta,
            double targetTemperature,
            double indoorTemperature,
            double ambientTemperature,
            double expectedSignal) {
    }

    /**
     * @return Stream of {@link TargetAdjustmentTestData} for {@link #targetAdjustmentCoolingTest(TargetAdjustmentTestData)}.
     */
    private static Stream<TargetAdjustmentTestData> targetAdjustmentCoolingProvider() {

        return Stream.of(
                new TargetAdjustmentTestData(HvacMode.COOLING, 1.0, 22.0, 25.0, 10.0, 14.0),
                new TargetAdjustmentTestData(HvacMode.COOLING, 1.0, 22.0, 23.0, 10.0, 12.0),
                new TargetAdjustmentTestData(HvacMode.COOLING, 1.0, 22.0, 22.5, 10.0, 5.75),
                new TargetAdjustmentTestData(HvacMode.COOLING, 1.0, 22.0, 22.0, 10.0, 0.0),
                new TargetAdjustmentTestData(HvacMode.COOLING, 1.0, 22.0, 21.0, 10.0, -10.0),

                // https://github.com/home-climate-control/dz/issues/263
                new TargetAdjustmentTestData(HvacMode.COOLING, 1.0, 22.0, 21.0, 30.0, -10.0),

                // https://github.com/home-climate-control/dz/issues/328
                // Note changeoverDelta == 0.
                // NEGATIVE_INFINITY is wrong, but this is what it is now. Will be adjusted after the fix is in.
                new TargetAdjustmentTestData(HvacMode.COOLING, 0.0, 22.0, 21.0, 20.0, 0.0),

                // https://github.com/home-climate-control/dz/issues/329
                // Note changeoverDelta == 0 && targetTemperature == indoorTemperature.
                new TargetAdjustmentTestData(HvacMode.COOLING, 0.0, 25.0, 25.0, 20.0, 5.0)
        );
    }

    /**
     * @return Stream of {@link TargetAdjustmentTestData} for {@link #targetAdjustmentHeatingTest(TargetAdjustmentTestData)}.
     */
    private static Stream<TargetAdjustmentTestData> targetAdjustmentHeatingProvider() {

        return Stream.of(
                new TargetAdjustmentTestData(HvacMode.HEATING, 1.0, 25.0, 23.0, 30.0, 6.0),
                new TargetAdjustmentTestData(HvacMode.HEATING, 1.0, 25.0, 24.0, 30.0, 5.0),
                new TargetAdjustmentTestData(HvacMode.HEATING, 1.0, 25.0, 24.5, 30.0, 2.25),
                new TargetAdjustmentTestData(HvacMode.HEATING, 1.0, 25.0, 25.0, 30.0, 0.0),
                new TargetAdjustmentTestData(HvacMode.HEATING, 1.0, 25.0, 25.5, 30.0, -1.75),
                new TargetAdjustmentTestData(HvacMode.HEATING, 1.0, 25.0, 26.0, 30.0, -3.0),

                // https://github.com/home-climate-control/dz/issues/328
                // Note changeoverDelta == 0.
                new TargetAdjustmentTestData(HvacMode.HEATING, 0.0, 25.0, 26.0, 30.0, 0.0),

                // https://github.com/home-climate-control/dz/issues/329
                // Note changeoverDelta == 0 && targetTemperature == indoorTemperature.
                new TargetAdjustmentTestData(HvacMode.HEATING, 0.0, 25.0, 25.0, 30.0, 5.0)
        );
    }
}
