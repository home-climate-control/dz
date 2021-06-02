package net.sf.dz3.device.actuator.impl;

import net.sf.dz3.device.actuator.HvacDriver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class HvacControllerImplTest {

    @Test
    void nullSignal() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    new HvacControllerImpl("name", mock(HvacDriver.class)).consume(null);
                })
                .withMessageStartingWith("signal can't be null");
    }
}
