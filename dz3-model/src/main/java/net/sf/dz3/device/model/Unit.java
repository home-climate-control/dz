package net.sf.dz3.device.model;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSource;
import com.homeclimatecontrol.jukebox.jmx.JmxAttribute;
import com.homeclimatecontrol.jukebox.jmx.JmxAware;

/**
 * HVAC unit abstraction.
 *
 * Listens to zone controller signal, and issues commands to the actual hardware driver.
 * 
 * This abstraction is not {@link HvacMode} aware, it can only support one mode - either heating, or cooling.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */
public interface Unit extends DataSource<UnitSignal>, DataSink<Double>, Comparable<Unit>, JmxAware {

    /**
     * Get the minimum runtime.
     * 
     * @return Minimum allowed runtime, in milliseconds.
     */
    @JmxAttribute(description="Minimum runtime allowed, milliseconds")
    long getMinRuntime();
   
   /**
    /**
     * Get the name of the unit.
     */
    @JmxAttribute(description="Unit name")
    String getName();
    
    /**
     * Consume a signal from the {@link ZoneController}.
     * 
     * @param signal Signal to consume. Can't be {@code null}, nor an error signal,
     * and can't have the sample value less than {@code 0}.
     * 
     * @exception IllegalArgumentException if the {@code signal} doesn't pass the sanity check. 
     */
    void consume(DataSample<Double> signal);
}
