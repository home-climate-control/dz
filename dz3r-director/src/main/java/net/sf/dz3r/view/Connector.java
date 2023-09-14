package net.sf.dz3r.view;

/**
 * An entity capable of representing the state and accepting control input at the same time.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public interface Connector extends MetricsCollector, ControlInput, AutoCloseable {
}
