package net.sf.dz3.device.actuator.servomaster;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DamperFactoryTest {
    
    private final Logger logger = LogManager.getLogger(getClass());

    @Test
    public void testBadClassName() {
        
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DamperFactory("badClass", null))
                .withMessageStartingWith("Can't find class for name");
    }

    @Test
    public void testNotController() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DamperFactory("java.lang.String", null))
                .withMessageStartingWith("Not a servo controller");
    }
}
