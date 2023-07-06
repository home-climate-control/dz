package net.sf.dz3r.jmx;

/**
 * Interface to be used to declare that the object <strong>needs</strong> to be exposed via JMX.
 *
 * Whether or not the specific class <strong>can</strong> be exposed via JMX is decided by the {@link JmxWrapper},
 * taking into account {@link JmxAttribute} annotations.
 *
 * Whether or not the specific <strong>instance</strong> of an object that <strong>can</strong> be exposed
 * <strong>will</strong> be exposed depends on the return value of {@link #getJmxDescriptor()}.
 *
 * <strong>Android note</strong>: since JMX on Android is not supported, it remains to be seen how exactly
 * this will be useful. For now, JMX support on Android will just be NOP - it is simpler to have it there
 * and support "write once, run everywhere" (even when using NOP JMX library)than to rip the JMX support
 * out of the code base.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2007-2012
 */
public interface JmxAware {

    /**
     * Get the JMX descriptor.
     *
     * @return The descriptor, or {@code null} if this specific instance of the object doesn't need to be exposed.
     */
    JmxDescriptor getJmxDescriptor();
}
