package net.sf.dz3r.jmx;

/**
 * The template definition for the handler that handles the {@link
 * javax.management.DynamicMBean#getAttribute} method.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2009
 * @since Jukebox 2.0p5
 */
public interface JmxAttributeReader {

    /**
     * Perform the <code>getAttribute()</code> operation on the target object.
     *
     * The attribute name is absent because this object is invoked as a
     * target from a {@link JmxHelper dispatcher}, and the attribute name is
     * implicitly present there.
     *
     * @param target The object to get the information from.
     *
     * @return The object that will be returned as a result of
     * <code>getAttribute()</code> call.
     */
    Object getAttribute(Object target);
}
