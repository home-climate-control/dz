package net.sf.dz3.device.sensor.impl;

import net.sf.dz3.device.sensor.AnalogSensor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConvertingSensorTest {

    @Test
    void nameCollision() {

        AnalogSensor source = new NullSensor("address", 0);
        AnalogSensor conversion = new ConvertingSensor(source, new AnalogConverterTMP36());

        assertThat(source).isNotEqualByComparingTo(conversion);
        assertThat(conversion).isNotEqualByComparingTo(source);
    }
}
