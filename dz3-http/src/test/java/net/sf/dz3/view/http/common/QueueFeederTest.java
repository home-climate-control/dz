package net.sf.dz3.view.http.common;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class QueueFeederTest {

    @Test
    public void testNullContext() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new QueueFeeder<String>((Map<String, Object>) null) {})
                .withMessage("null context, doesn't make sense");
    }

    @Test
    public void testNullQueue() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new QueueFeeder<String>(new HashMap<String, Object>()) {})
                .withMessage("null queue, doesn't make sense");
    }
}
