package net.sf.dz3r.jmx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanInfo;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * The helper class allowing the {@link DynamicMBean} operations to be
 * performed by the set of simple handler objects. Uses the
 * {@code dispatcher} pattern.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2018
 * @since Jukebox 2.0p5
 */
public class JmxHelper implements DynamicMBean {

    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * Exception message used in multiple methods.
     */
    public static final String NO_NULLS = "Don't accept nulls";

    /**
     * Object to perform the operations on. Note that in the case when the
     * handlers are implemented as anonymous inner classes, this object is not
     * necessary. It is present here just in case when the target object
     * operations become complicated, or the handler class may be reused by
     * multiple classes, when the implementation of the handler as anonymous
     * class is not applicable or sufficient.
     */
    protected Object target;

    /**
     * The management signature returned by {@link #getMBeanInfo getMBeanInfo()}
     * method.
     */
    protected MBeanInfo info;

    /**
     * Attribute name to getter handler mapping.
     */
    protected Map<String, JmxAttributeReader> readerMap = new TreeMap<String, JmxAttributeReader>();

    /**
     * Attribute name to setter handler mapping.
     */
    protected Map<String, JmxAttributeWriter> writerMap = new TreeMap<String, JmxAttributeWriter>();

    /**
     * Action name to invocation handler mapping.
     */
    protected Map<String, JmxInvocationHandler> actionMap = new TreeMap<String, JmxInvocationHandler>();

    /**
     * Create the instance.
     *
     * @param target Object to perform the operations on.
     * @param info The management signature.
     */
    public JmxHelper(Object target, MBeanInfo info) {

        if (target == null || info == null) {

            throw new IllegalArgumentException(NO_NULLS);
        }

        this.target = target;
        this.info = info;
    }

    /**
     * Register the getter handler.
     *
     * @param key The attribute name
     * @param reader The getter handler.
     */
    public void put(String key, JmxAttributeReader reader) {

        if (key == null || reader == null) {

            throw new IllegalArgumentException(NO_NULLS);
        }

        readerMap.put(key, reader);
    }

    /**
     * Register the setter handler.
     *
     * @param key The attribute name
     * @param writer The setter handler.
     */
    public void put(String key, JmxAttributeWriter writer) {

        if (key == null || writer == null) {

            throw new IllegalArgumentException(NO_NULLS);
        }

        writerMap.put(key, writer);
    }

    /**
     * Register the invocation handler.
     *
     * @param key The action name
     * @param handler The invocation handler.
     */
    public void put(String key, JmxInvocationHandler handler) {

        if (key == null || handler == null) {

            throw new IllegalArgumentException(NO_NULLS);
        }

        actionMap.put(key, handler);
    }

    @Override
    public Object getAttribute(String attribute) {

        if (attribute == null || "".equals(attribute)) {

            throw new IllegalArgumentException(NO_NULLS);
        }

        JmxAttributeReader r = readerMap.get(attribute);

        if (r == null) {

            throw new IllegalArgumentException("Reader for attribute '" + attribute + "' is not defined");
        }

        return r.getAttribute(target);
    }

    @Override
    public void setAttribute(Attribute attribute) {

        if (attribute == null) {

            throw new IllegalArgumentException(NO_NULLS);
        }

        JmxAttributeWriter w = writerMap.get(attribute.getName());

        if (w == null) {

            throw new IllegalArgumentException("Writer for attribute '" + attribute.getName() + "' is not defined");
        }

        w.setAttribute(target, attribute);
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {

        AttributeList result = new AttributeList();

        for (int idx = 0; idx < attributes.length; idx++) {

            String next = attributes[idx];

            try {

                Attribute attr = new Attribute(next, getAttribute(next));
                result.add(attr);

            } catch (Throwable t) {

                logger.error("Getting '" + next + "' attribute failed, cause:", t);
            }
        }

        return result;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {

        AttributeList result = new AttributeList();

        for (Iterator<Attribute> i = attributes.asList().iterator(); i.hasNext();) {

            Attribute next = i.next();

            try {

                setAttribute(next);
                result.add(next);

            } catch (Throwable t) {

                logger.error("Setting '" + next.getName() + "' attribute failed, cause:", t);
            }
        }

        return result;
    }

    @Override
    public MBeanInfo getMBeanInfo() {

        return info;
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) {

        JmxInvocationHandler h = actionMap.get(actionName);

        if (h == null) {

            throw new IllegalArgumentException("Invocation handler for action '" + actionName + "' is not defined");
        }

        return h.invoke(target, params, signature);
    }
}
