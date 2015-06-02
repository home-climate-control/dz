package net.sf.dz3.device.sensor.impl.tcp.server;

import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

public class SensorServerTest extends TestCase {
    
    public void testInstantiation() throws Throwable {

        Set<String> addresses = new TreeSet<String>();
        SensorServer ss = new SensorServer(addresses, 0, 0);
        
        ss.cleanup();
    }
}
