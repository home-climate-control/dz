package net.sf.dz3r.signal.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;

class AnalogConverterLM34Test {

    protected final Logger logger = LogManager.getLogger(getClass());

    private final AnalogConverter c = new AnalogConverterLM34();

    @Test
    void testLM34High() {

        // High boundary conversion failed
        assertThat(c.convert(3000d)).isEqualTo(148.889, within(0.001));
    }

    @Test
    void testLM34Middle() {

        // Midrange conversion failed
        assertThat(c.convert(720d)).isEqualTo(22.222, within(0.001));
    }

    @Test
    void testLM34Low() {

        // Low boundary conversion failed
        assertThat(c.convert(-500d)).isEqualTo(-45.556, within(0.001));
    }

    @Test
    void testLM34AnalogReference() {

        ThreadContext.push("LM34");

        try {

            assertThatCode(() -> {
                printC(1100d);
                printC(2560d);
                printC(5000d);
            }).doesNotThrowAnyException();

        } finally {
            ThreadContext.pop();
        }
    }

    private void printC(double millivolts) {
        logger.info("Top measurable temperature at " + millivolts + "mV analog reference: " + c.convert(millivolts) + "°C");
    }
}
