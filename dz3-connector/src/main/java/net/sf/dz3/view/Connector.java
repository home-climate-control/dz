package net.sf.dz3.view;

import com.homeclimatecontrol.jukebox.jmx.JmxAware;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base class for implementing the V in MVC.
 *
 * @param <T> Component type to use.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class Connector<T> implements JmxAware {

    protected final Logger logger = LogManager.getLogger(getClass());

    /**
     * Component factory map.
     *
     * The key is the class of the object to be taken from the {@link #initSet},
     * the value is the component factory instance to take care of its creation.
     */
    private final Map<Class<?>, ConnectorFactory<T>> factoryMap = new HashMap<>();

    /**
     * Copy of the initial set of objects passed via constructor.
     */
    private final Set<Object> initSet = new HashSet<>();

    /**
     * Mapping from the object in {@link #initSet} to the component that represents it.
     */
    private final Map<Object, T> componentMap = new LinkedHashMap<>();

    protected Connector(Set<Object> initSet) {
        this(initSet, null);
    }

    protected  Connector(Set<Object> initSet, Set<ConnectorFactory<T>> factorySet) {

        ThreadContext.push("Connector()");

        try {

            if (initSet != null) {
                this.initSet.addAll(initSet);
            }

            if (factorySet == null) {
                return;
            }

            for (ConnectorFactory<T> factory : factorySet) {

                logger.info("Using custom factory {} for {}",
                        factory.getClass().getName(),
                        factory.getSourceClass().getName());

                register(factory.getSourceClass(), factory);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    public final void register(Class<?> componentClass, ConnectorFactory<T> connector) {
        factoryMap.put(componentClass, connector);
    }

    /**
     * Keep invoking factories from {@link #factoryMap} on objects from {@link #initSet}
     * until they're all accounted for, and put them into {@link #componentMap}.
     */
    private Map<Object, T> createComponentMap(Map<String, Object> context) {

        ThreadContext.push("createComponentMap");

        try {

            Map<Object, T> result = new LinkedHashMap<>();

            for (Object initObject : initSet) {

                Class<?> initClass = initObject.getClass();
                ConnectorFactory<T> factory = factoryMap.get(initClass);

                if (factory == null) {

                    logger.info("No direct mapping for {}, searching parents", initClass.getName());

                    for (Map.Entry<Class<?>, ConnectorFactory<T>> class2factory : factoryMap.entrySet()) {

                        if (class2factory.getKey().isAssignableFrom(initClass)) {
                            factory = class2factory.getValue();
                            logger.info("Substitute: {} isA {}", initClass.getName(), class2factory.getKey().getName());
                        }
                    }
                }

                if (factory == null) {
                    logger.error("Don't know how to handle {}: {}", initClass.getName(), initObject);
                    continue;
                }

                try {

                    var component = factory.createComponent(initObject, context);

                    logger.info("Created {}  for {}", component, initObject);
                    result.put(initObject, component);

                } catch (Throwable t) { // NOSONAR Consequences have been considered
                    // Not a fatal condition, there are other components
                    logger.error("Failed to create a component for {}, moving on", initObject, t);
                }
            }

            if (result.isEmpty()) {
                // Not fatal either
                logger.warn("componentMap is empty, did you specify any arguments in the constructor?");
            }

            return result;

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Create the context.
     *
     * This object will be used by {@link #createComponentMap(Map)} and discarded. Don't bother keeping
     * the reference, it won't be called often, most probably once.
     *
     * @return The context.
     */
    protected abstract Map<String, Object> createContext();

    protected Set<Object> getInitSet() {
        return Collections.unmodifiableSet(initSet);
    }

    protected final Map<Object, T> getComponentMap() {
        return Collections.unmodifiableMap(componentMap);
    }

    /**
     * Activate the connector.
     */
    public final synchronized void activate() {

        ThreadContext.push("activate");

        try {

            if (!componentMap.isEmpty()) {
                // Last deactivate() wasn't clean, this is a problem
                throw new IllegalStateException("componentMap is not empty: " + componentMap);
            }

            componentMap.putAll(createComponentMap(createContext()));
            activate2();

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Deactivate the connector.
     */
    public final synchronized void deactivate() {

        ThreadContext.push("deactivate");

        try {

            deactivate2();
            componentMap.clear();

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Give subclasses a chance to do what they need.
     */
    protected abstract void activate2();

    /**
     * Give subclasses a chance to do what they need.
     */
    protected abstract void deactivate2();
}
