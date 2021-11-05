package net.sf.dz3r.signal.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class AnalogConverterTMP36Test {

    protected final Logger logger = LogManager.getLogger(getClass());

    private final AnalogConverter c = new AnalogConverterTMP36();

    @Test
    void testTMP36AnalogReference() {

        ThreadContext.push("TMP36");

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
