package net.sf.dz3r.jmx;

import javax.management.DynamicMBean;

/**
 * The template definition for the handler that handles the {@link DynamicMBean#invoke(String, Object[], String[])} method.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2009
 * @since Jukebox 2.0p5
 */
abstract public class JmxInvocationHandler {

    /**
     * Perform the <code>invoke()</code> operation on the target object.
     *
     * The action name is absent because this object is invoked as a target
     * from a {@link JmxHelper dispatcher}, and the action name is
     * implicitly present there.
     *
     * @param target The object to get the information from.
     *
     * @param params Call parameters
     *
     * @param signature Call signature
     *
     * @return The object that will be returned as a result of
     * <code>invoke()</code> call.
     */
    abstract public Object invoke(Object target, Object params[], String signature[]);
}
