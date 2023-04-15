package net.sf.dz3r.device.actuator.economizer;

import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.device.actuator.NullSwitch;
import net.sf.dz3r.device.actuator.Switch;
import net.sf.dz3r.model.HvacMode;
import net.sf.dz3r.signal.Signal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class AbstractEconomizerTest {

    @ParameterizedTest
    @MethodSource("targetAdjustmentProvider")
    void targetAdjustmentTest(TestData source) {

        var settings = new EconomizerSettings(
                source.mode,
                source.changeoverDelta,
                source.targetTemperature,
                true,
                1.0, 0.0001, 1.0);

        var e = new TestEconomizer("eco", settings, Flux.empty(), new NullSwitch(""));

        var signal = e.computeCombined(source.indoorTemperature, source.ambientTemperature);

        assertThat(signal).isEqualTo(source.expectedSignal);

    }

    private class TestEconomizer extends AbstractEconomizer<String> {

        /**
         * Create an instance.
         * <p>
         * Note that only the {@code ambientFlux} argument is present, indoor flux is provided to {@link #compute(Flux)}.
         *
         * @param name
         * @param settings
         * @param ambientFlux  Flux from the ambient temperature sensor.
         * @param targetDevice Switch to control the economizer actuator.
         */
        protected TestEconomizer(String name, EconomizerSettings settings, Flux<Signal<Double, Void>> ambientFlux, Switch<String> targetDevice) {
            super(name, settings, ambientFlux, targetDevice);
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
    private static final class TestData {

        public final HvacMode mode;
        public final double changeoverDelta;
        public final double targetTemperature;
        public final double indoorTemperature;
        public final double ambientTemperature;
        public final double expectedSignal;

        private TestData(HvacMode mode, double changeoverDelta, double targetTemperature, double indoorTemperature, double ambientTemperature, double expectedSignal) {
            this.mode = mode;
            this.changeoverDelta = changeoverDelta;
            this.targetTemperature = targetTemperature;
            this.indoorTemperature = indoorTemperature;
            this.ambientTemperature = ambientTemperature;
            this.expectedSignal = expectedSignal;
        }
    }

    /**
     * @return Stream of {@link TestData} for {@link #targetAdjustmentTest(TestData)}.
     */
    private static Stream<TestData> targetAdjustmentProvider() {

        return Stream.of(
                new TestData(HvacMode.COOLING, 1.0, 22.0, 25.0, 10.0, 14.0),
                new TestData(HvacMode.COOLING, 1.0, 22.0, 23.0, 10.0, 12.0),
                new TestData(HvacMode.COOLING, 1.0, 22.0, 22.5, 10.0, 5.75),
                new TestData(HvacMode.COOLING, 1.0, 22.0, 22.0, 10.0, 0.0),
                new TestData(HvacMode.COOLING, 1.0, 22.0, 21.0, 10.0, -10.0)
        );

    }

}
