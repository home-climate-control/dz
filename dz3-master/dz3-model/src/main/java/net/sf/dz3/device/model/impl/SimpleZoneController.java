package net.sf.dz3.device.model.impl;

import java.util.Set;

import net.sf.dz3.device.model.Thermostat;

/**
 * A simple A/C zone controller.
 *
 * <p>
 *
 * Doesn't do anything fancy, just switches on the A/C when one or more of
 * the thermostats issues the signal with the value more than 1.0, and shuts
 * it off when all the thermostats issue the signal less than -1.0. As the
 * value of the signal raises above 1.0, the dampers are being open, as it
 * falls below -1.0, the dampers are being closed.
 *
 * <p>
 *
 * <strong>Correction:</strong> those -1.0 and 1.0 not absolute, but
 * relative - they have to be adjusted depending on the A/C mode.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class SimpleZoneController extends AbstractZoneController {
    
    /**
     * Create an instance with no connected thermostats.
     * 
     * @param name Zone controller name.
     */
    public SimpleZoneController(String name) {
        super(name);
    }

    /**
     * Create an instance with connected thermostats.
     * 
     * @param name Zone controller name.
     * @param sources Thermostats to use as signal sources.
     */
    public SimpleZoneController(String name, Set<Thermostat> sources) {
        
        super(name, sources);
    }
}
