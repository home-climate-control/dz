package net.sf.dz3r.runtime.config.hardware;


import net.sf.dz3r.runtime.config.Identifiable;

/**
 * The only purpose of this interface is to dodge the fact that records are not polymorphic for one special case.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public interface GenericHvacDeviceConfig extends Identifiable, FilterAware {
}
