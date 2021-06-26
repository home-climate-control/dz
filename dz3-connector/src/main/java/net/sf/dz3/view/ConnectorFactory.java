package net.sf.dz3.view;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Connector factory base.
 *
 * @param <T> Component type to create.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class ConnectorFactory<T> {

    protected final Logger logger = LogManager.getLogger();

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
    public abstract T createComponent(
            Object source,
            Map<String, Object> context);
}
