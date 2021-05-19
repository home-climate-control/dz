package net.sf.dz3.view;

import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;

public class ConnectorTest {

    @Test
    public void testNullInitSet() {

        new TestConnector(null);
        assertTrue(true);
    }

    @Test
    public void testNullFactorySet() {

        new TestConnector(null, null);
        assertTrue(true);
    }

    private class TestConnector extends Connector<String> {

        public TestConnector(Set<Object> initSet) {
            super(initSet);
        }

        public TestConnector(Set<Object> initSet, Set<ConnectorFactory<String>> factorySet) {
            super(initSet, factorySet);
        }

        @Override
        public JmxDescriptor getJmxDescriptor() {
            throw new IllegalStateException("Not Implemented");
        }

        @Override
        protected Map<String, Object> createContext() {
            throw new IllegalStateException("Not Implemented");
        }

        @Override
        protected void activate2() {
            // NOP
        }

        @Override
        protected void deactivate2() {
            // NOP
        }
    }
}
