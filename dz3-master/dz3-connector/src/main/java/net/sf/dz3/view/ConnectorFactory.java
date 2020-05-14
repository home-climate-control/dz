package net.sf.dz3.view;

import java.util.Map;

import net.sf.jukebox.logger.LogAware;

/**
 * Connector factory base.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public abstract class ConnectorFactory<ComponentConnector> extends LogAware {

    /**
     * Get the base class or interface of the object that the component
     * created by this class will represent.
     *
     * This is necessary for advanced configuration.
     *
     * @return Base class or interface of the represented object.
     */
    public abstract Class<?> getSourceClass();

    /**
     * Create a component for the given source object,
     * and link it to the source object if necessary.
     *
     * @param source One of DZ objects.
     * @param context Necessary common objects as {@code key=value}.
     *
     * @return The connector to represent the source object.
     */
    public abstract ComponentConnector createComponent(
            Object source,
            Map<String, Object> context);
}
