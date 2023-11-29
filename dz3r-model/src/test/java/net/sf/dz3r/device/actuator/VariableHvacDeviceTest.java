package net.sf.dz3r.device.actuator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class VariableHvacDeviceTest {

    private final static Random rg = new Random();

    @ParameterizedTest
    @MethodSource("getBandData")
    void testBands(BandData item) {

        assertThat(VariableHvacDevice.band(item.value, item.bandCount)).isEqualTo(item.expected);
    }

    private record BandData(
            double value,
            int bandCount,
            double expected
    ) {

    }
    static Stream<BandData> getBandData() {

        var randomInput = rg.nextDouble();

        return Stream.of(
                new BandData(randomInput, 0, randomInput),
                new BandData(0, rg.nextInt(0, 101), 0),
                new BandData(1, rg.nextInt(0, 101), 1),
                new BandData(0.25, 4, 0.25),
                new BandData(0.0001, 4, 0.25),
                new BandData(0.26, 4, 0.5)
        );
    }
}
