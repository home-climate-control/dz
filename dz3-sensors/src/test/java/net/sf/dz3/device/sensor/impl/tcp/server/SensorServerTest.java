package net.sf.dz3.device.sensor.impl.tcp.server;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

class SensorServerTest {

    @Test
    public void testInstantiation() throws Throwable {

        Set<String> addresses = new TreeSet<String>();
        SensorServer ss = new SensorServer(addresses, 0, 0);
        
        ss.cleanup();
    }
}
