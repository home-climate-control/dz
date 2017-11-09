package net.sf.dz3.view.http.common;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class QueueFeederTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testNullContext() {

        Map<String, Object> context = null;

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null context, doesn't make sense");

        QueueFeeder<String> qf = new QueueFeeder<String>(context) {};
    }

    @Test
    public void testNullQueue() {

        Map<String, Object> context = new HashMap<String, Object>();

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null queue, doesn't make sense");

        QueueFeeder<String> qf = new QueueFeeder<String>(context) {};
    }
}
