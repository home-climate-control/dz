package net.sf.dz3.view;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.homeclimatecontrol.jukebox.jmx.JmxAware;

/**
 * Base class for implementing the V in MVC.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
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
    private final Map<Object, T> componentMap = new HashMap<>();

    public Connector(Set<Object> initSet) {

        this(initSet, null);
    }

    public Connector(Set<Object> initSet, Set<ConnectorFactory<T>> factorySet) {

        ThreadContext.push("Connector()");

        try {

            if (initSet != null) {

                this.initSet.addAll(initSet);
            }

            if (factorySet == null) {

                return;
            }

            for (Iterator<ConnectorFactory<T>> i = factorySet.iterator(); i.hasNext(); ) {

                ConnectorFactory<T> factory = i.next();

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
    private void createComponentMap(Map<String, Object> context) {

        ThreadContext.push("createComponentMap");

        try {

            for (Iterator<Object> i = initSet.iterator(); i.hasNext(); ) {

                Object initObject = i.next();
                Class<?> initClass = initObject.getClass();
                ConnectorFactory<T> factory = factoryMap.get(initClass);

                if (factory == null) {

                    logger.info("No direct mapping for {}, searching parents", initClass.getName());

                    for (Iterator<Class<?>> i2 = factoryMap.keySet().iterator(); i2.hasNext(); ) {

                        Class<?> c = i2.next();

                        if (c.isAssignableFrom(initClass)) {

                            factory = factoryMap.get(c);

                            logger.info("Subsitute: {} isA {}", initClass.getName(), c.getName());
                        }
                    }
                }

                if (factory == null) {

                    logger.error("Don't know how to handle {}: {}", initClass.getName(), initObject);
                    continue;

                }

                try {

                    T c = factory.createComponent(initObject,context);

                    logger.info("Created {}  for {}", c, initObject);
                    componentMap.put(initObject, c);

                } catch (Throwable t) {
                    // Not a fatal condition, there are other components
                    logger.error("Failed to create a component for " + initObject + ", moving on", t);
                }
            }

            if (componentMap.isEmpty()) {

                // Not fatal either
                logger.warn("componentMap is empty, did you specify any arguments in the constructor?");
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Create the context.
     *
     * This object will be used by {@link #createComponentMap(TreeMap)} and discarded. Don't bother keeping
     * the reference, it won't be called often, most probably once.
     *
     * @return The context.
     */
    protected abstract Map<String, Object> createContext();

    protected Set<Object> getInitSet() {

        return Collections.unmodifiableSet(initSet);
    }

    /**
     * @deprecated Need to replace by a lookup method.
     */
    @Deprecated
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

            createComponentMap(createContext());

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
