package net.sf.dz3r.device.actuator;

import net.sf.dz3r.device.Addressable;

/**
 * Single channel switch abstraction.
 *
 * Unlike {@link Switch}, this specification honors <a href="https://martinfowler.com/bliki/CQRS.html">Command Query Responsibility Segregation</a>
 * (like {@link VariableOutputDevice} does).
 *
 * The idea is to replicate the functionality of the {@link Switch} hierarchy in a CQRS compliant way, then retire {@link Switch}
 * and rename the classes of this hierarchy.
 *
 * @param <A> Address type.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko 2001-2023
 */
public interface CqrsSwitch<A extends Comparable<A>> extends Addressable<A> {
}
