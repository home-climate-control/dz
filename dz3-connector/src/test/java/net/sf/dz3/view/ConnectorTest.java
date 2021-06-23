package net.sf.dz3.view;

import com.homeclimatecontrol.jukebox.jmx.JmxDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectorTest {

    @Test
    void testNullInitSet() {
        assertThat(new TestConnector(null)).isNotNull();
    }

    @Test
    void testNullFactorySet() {
        assertThat(new TestConnector(null, null)).isNotNull();
    }

    private static class TestConnector extends Connector<String> {

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
