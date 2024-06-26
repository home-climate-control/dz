package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.device.actuator.HvacDevice;
import net.sf.dz3r.device.actuator.NullCqrsSwitch;
import net.sf.dz3r.device.actuator.SwitchableHvacDevice;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class AbstractEconomizerTest {

    /**
     * Make sure that control signal is computed properly as the indoor temperature is approaching the {@link EconomizerSettings#targetTemperature}.
     */
    @ParameterizedTest
    @MethodSource("targetAdjustmentProvider")
    void targetAdjustmentTest(TargetAdjustmentTestData source) {

        var config = new EconomizerConfig(
                source.mode,
                1.0, 0.0001, 1.0,
                new EconomizerSettings(
                        source.changeoverDelta,
                        source.targetTemperature,
                        true,
                        1.0
                )
        );

        var e = new TestEconomizer(
                "eco",
                config,
                new SwitchableHvacDevice(
                        Clock.systemUTC(),
                        "d",
                        HvacMode.COOLING,
                        new NullCqrsSwitch("s"),
                        false,
                        null)
        );

        var signal = e.computeCombined(source.indoorTemperature, source.ambientTemperature);

        assertThat(signal).isEqualTo(source.expectedSignal);

    }

    private class TestEconomizer extends AbstractEconomizer {

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
    private static final class TargetAdjustmentTestData {

        public final HvacMode mode;
        public final double changeoverDelta;
        public final double targetTemperature;
        public final double indoorTemperature;
        public final double ambientTemperature;
        public final double expectedSignal;

        private TargetAdjustmentTestData(HvacMode mode, double changeoverDelta, double targetTemperature, double indoorTemperature, double ambientTemperature, double expectedSignal) {
            this.mode = mode;
            this.changeoverDelta = changeoverDelta;
            this.targetTemperature = targetTemperature;
            this.indoorTemperature = indoorTemperature;
            this.ambientTemperature = ambientTemperature;
            this.expectedSignal = expectedSignal;
        }
    }

    /**
     * @return Stream of {@link TargetAdjustmentTestData} for {@link #targetAdjustmentTest(TargetAdjustmentTestData)}.
     */
    private static Stream<TargetAdjustmentTestData> targetAdjustmentProvider() {

        return Stream.of(
                new TargetAdjustmentTestData(HvacMode.COOLING, 1.0, 22.0, 25.0, 10.0, 14.0),
                new TargetAdjustmentTestData(HvacMode.COOLING, 1.0, 22.0, 23.0, 10.0, 12.0),
                new TargetAdjustmentTestData(HvacMode.COOLING, 1.0, 22.0, 22.5, 10.0, 5.75),
                new TargetAdjustmentTestData(HvacMode.COOLING, 1.0, 22.0, 22.0, 10.0, 0.0),
                new TargetAdjustmentTestData(HvacMode.COOLING, 1.0, 22.0, 21.0, 10.0, -10.0),

                // https://github.com/home-climate-control/dz/issues/263
                new TargetAdjustmentTestData(HvacMode.COOLING, 1.0, 22.0, 21.0, 30.0, -10.0)
        );

    }

}
