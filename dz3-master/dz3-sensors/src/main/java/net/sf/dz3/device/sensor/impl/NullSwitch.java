package net.sf.dz3.device.sensor.impl;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.sf.dz3.device.sensor.Switch;
import net.sf.jukebox.datastream.signal.model.DataSink;
import net.sf.jukebox.jmx.JmxAware;
import net.sf.jukebox.jmx.JmxDescriptor;

/**
 * Null switch.
 * 
 * Does absolutely nothing other than reflecting itself in the log and via JMX.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko 2001-2019
 */
public class NullSwitch extends AbstractSwitch {
    
    /**
     * Create an instance.
     * 
     * @param address Address to use.
     */
    public NullSwitch(String address) {
        super(address, false);
    }
    
    @Override
    public JmxDescriptor getJmxDescriptor() {

        return new JmxDescriptor(
                "dz",
                "Null Switch",
                Integer.toHexString(hashCode()),
                "Pretends to be the actual switch");
    }
}
