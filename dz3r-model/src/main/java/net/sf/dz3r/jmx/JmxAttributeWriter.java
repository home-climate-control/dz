package net.sf.dz3r.jmx;

import javax.management.Attribute;

/**
 * The template definition for the handler that handles the {@link
 * javax.management.DynamicMBean#setAttribute
 * javax.management.DynamicMBean#setAttribute} method.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2009
 * @since Jukebox 2.0p5
 */
public interface JmxAttributeWriter {

    /**
     * Perform the <code>setAttribute()</code> operation on the target object.
     *
     * The attribute name is absent because this object is invoked as a
     * target from a {@link JmxHelper dispatcher}, and the attribute name is
     * implicitly present there.
     *
     * @param target The object to get the information from.
     *
     * @param attribute Attribute to set.
     */
    void setAttribute(Object target, Attribute attribute);
}
