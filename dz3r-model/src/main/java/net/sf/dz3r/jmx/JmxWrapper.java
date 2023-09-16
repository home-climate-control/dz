package net.sf.dz3r.jmx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * A facility to expose objects presented to it via JMX.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2008-2020
 */
public final class JmxWrapper {

    public final Logger logger = LogManager.getLogger(getClass());

    private static final String NULL_TARGET = "target can't be null";
    private static final String UNSUPPORTED = "Not Supported Yet";

    /**
     * Create an instance.
     */
    public JmxWrapper() {

    }

    /**
     * Create an instance and register all given objects.
     *
     * @param targetSet Set of objects to register.
     */
    public JmxWrapper(Set<?> targetSet) {
        for (Object o : targetSet) {
            register(o);
        }
    }

    /**
     * Register the object with the JMX server.
     *
     * @param target Object to register.
     */
    public void register(Object target) {

        if (target == null) {
            throw new IllegalArgumentException(NULL_TARGET);
        }

        ObjectName name = null;
        MBeanServer mBeanServer = null;

        ThreadContext.push("jmxRegister");

        try {

            if (!(target instanceof JmxAware)) {
                logger.warn("Not JmxAware, ignored: {}", target);
                return;
            }

            var jmxDescriptor = ((JmxAware) target).getJmxDescriptor();

            if (jmxDescriptor == null) {
                throw new IllegalArgumentException("JMX Descriptor can't be null");
            }

            try {
                mBeanServer = ManagementFactory.getPlatformMBeanServer();

                var pattern = jmxDescriptor.domainName + ":" +
                        "name=" + jmxDescriptor.name + "," +
                        "instance=" + jmxDescriptor.instance;
                logger.info("name: {}", pattern);

                name = new ObjectName(pattern);

                expose(target, name, jmxDescriptor.description);

            } catch (InstanceAlreadyExistsException ex) {

                logger.info("Already registered: ", ex);

                ThreadContext.push("again");
                try {

                    // VT: FIXME: Need to change the scope of try/catch to include retrieval of
                    // JmxDescriptor so it can be reused here
                    mBeanServer.unregisterMBean(name);
                    expose(this, name, "FIXME");

                } catch (Throwable t) { // NOSONAR Consequences have been considered
                    logger.error("Failed", t);
                } finally {
                    ThreadContext.pop();
                }

            } catch (Throwable t) { // NOSONAR Consequences have been considered
                logger.error("Failed for {} {}", target.getClass().getName(), target, t);
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Expose the object via JMX, as appropriate.
     *
     * If the object doesn't have any {@link JmxAttribute JMX properties}, the method will silently return.
     *
     * @param target Object to analyze and expose.
     * @param name Object name.
     * @param description Object description.
     *
     * @exception IllegalArgumentException if there are inconsistencies in the way the {@link JmxAttribute properties}
     * are described, or the {@code target} is {@code null}.
     * @exception MBeanRegistrationException if there's a problem registering the {@code target}.
     * @exception NotCompliantMBeanException if the {@code target} is not quite what's needed.
     * @exception InstanceAlreadyExistsException if the {@code name} has already been registered.
     */
    public void expose(Object target, ObjectName name, String description) throws NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException {

        if (target == null) {
            throw new IllegalArgumentException(NULL_TARGET);
        }

        ThreadContext.push("expose(" + target.getClass().getSimpleName() + ')');

        try {

            var accessors = new ArrayList<MBeanAttributeInfo>();
            var accessor2mutator = new HashMap<Method, Method>();
            var targetClass = target.getClass();

            for (Method method : targetClass.getMethods()) {

                logger.debug("analyzing {}", method);

                var annotation = getAnnotation(targetClass, method, JmxAttribute.class);

                if (annotation != null) {
                    exposeMethod(target, method, (JmxAttribute) annotation, accessors, accessor2mutator);
                }
            }

            var attributeArray = accessors.toArray(new MBeanAttributeInfo[] {});
            var mbInfo = new MBeanInfo(target.getClass().getName(),
                    description,
                    attributeArray,
                    null,
                    null,
                    null,
                    null);
            var proxy = new Proxy(target, mbInfo, accessor2mutator);

            var mBeanServer = ManagementFactory.getPlatformMBeanServer();

            mBeanServer.registerMBean(proxy, name);

        } finally {
            ThreadContext.pop();
        }
    }

    private Annotation getAnnotation(Class<?> targetClass, Method method, Class<? extends Annotation> annotationClass) {

        ThreadContext.push("getAnnotation");

        try {

            logger.debug("method {}", method);
            logger.debug("annotation {}", annotationClass.getSimpleName());

            { // NOSONAR
                var annotation = method.getAnnotation(annotationClass);

                if (annotation != null) {
                    // This is the simple case...
                    logger.debug("simple case");
                    return annotation;
                }
            }

            // Well, simple case didn't work. Three options are possible now:
            // a) the annotation is present on the interface;
            // b) the annotation is present on the superclass (FIXME: make sure both abstract and concrete are covered);
            // c) it is not present at all.

            var interfaces = targetClass.getInterfaces();
            logger.debug("Checking {} interface[s]", interfaces.length);

            for (var anInterface : interfaces) {

                if (anInterface.equals(JmxAware.class)) {
                    // Skip without further ado
                    continue;
                }

                logger.debug("Checking interface {}", anInterface.getName());
                var annotation = getAnnotation(anInterface, method.getName(), annotationClass);

                if (annotation != null) {
                    return annotation;
                }
            }

            var superClass = targetClass.getSuperclass();

            if (superClass == null) {
                return null;
            }

            var superName = superClass.getName();

            if (superName.startsWith("java.")) {
                // Skip without further ado
                return null;
            }

            logger.info("Checking superclass: {}", superName);

            return getAnnotation(superClass, method.getName(), annotationClass);

        } finally {
            ThreadContext.pop();
        }
    }

    private Annotation getAnnotation(Class<?> targetClass, String methodName, Class<? extends Annotation> annotationClass) {

        ThreadContext.push("getAnnotation(" + methodName + ')');

        try {

            return getAnnotation(targetClass, targetClass.getMethod(methodName), annotationClass);

        } catch (NoSuchMethodException ignored) {

            // Oh well...
            logger.debug("no");
            return null;

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Expose a method off the target.
     *
     * @param target Object to expose the method of.
     * @param method The method to expose.
     *
     * @param annotation Annotation to extract the metadata from.
     * @param accessors Container to add found accessor methods to.
     * @param accessor2mutator Mapping from accessor to mutator method.
     */
    private void exposeMethod(Object target, Method method, JmxAttribute annotation, List<MBeanAttributeInfo> accessors, Map<Method, Method> accessor2mutator) {

        ThreadContext.push("exposeMethod");

        logger.debug("{}#{} found to be JmxAttribute", target.getClass().getName(), method.getName());

        try {

            String accessorName = resolveAccessorName(method);

            logger.debug("{}#{} exposed as {}", target.getClass().getName(), method.getName(), accessorName);
            logger.info("Description: {}", annotation.description());
            Method mutator = resolveMutator(target, method, accessorName);

            if (mutator != null) {
                accessor2mutator.put(method, mutator);
            }

            accessors.add(exposeJmxAttribute(accessorName, annotation.description(), method, mutator));

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Expose the method[s] via JMX.
     *
     * @param name Name to expose as.
     * @param description Human readable description.
     * @param accessor Accessor method.
     * @param mutator Matching mutator method, or {@code null} if none available.
     *
     * @return Operation signature.
     */
    private MBeanAttributeInfo exposeJmxAttribute(
            String name,
            String description,
            Method accessor, Method mutator) {

        ThreadContext.push("exposeJmxAttribute");

        try {

            logger.info("name:     {}", name);
            logger.info("type:     {}", accessor.getReturnType().getName());
            logger.info("accessor: {}", accessor);
            logger.info("mutator:  {}", mutator);

            var accessorInfo = new MBeanAttributeInfo(
                    name,
                    accessor.getReturnType().getName(),
                    description,
                    true,
                    mutator != null,
                    accessor.getName().startsWith("is"));

            logger.debug("accessor: {}", accessorInfo);

            return accessorInfo;

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Resolve a mutator method matching the accessor method in type and name.
     *
     * @param target Object to resolve the method on.
     * @param accessor Accessor method to find the match for.
     * @param name Exposed method name. @return Matching mutator method, or {@code null}, if none is found.
     *
     * @return The method found, or {@code null} if none.
     */
    private Method resolveMutator(Object target, Method accessor, String name) {
        ThreadContext.push("resolveMutator(" + name + ")");
        try {

            Class<?> returnType = accessor.getReturnType();
            String mutatorName = "set" + upperFirst(name);

            logger.debug("Trying {}({})", mutatorName, returnType.getSimpleName());

            var mutator = target.getClass().getMethod(mutatorName, returnType);

            logger.debug("Found: {}", mutator);

            return mutator;

        } catch (NoSuchMethodException e) {
            // This is normal
            logger.info("No mutator found: {}", e.getMessage());
            return null;
        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Resolve the attribute name.
     *
     * @param method Method to resolve the exposed name of.
     *
     * @return Name to expose as.
     *
     * @exception IllegalStateException if the method is not an accessor (is void or has arguments).
     */
    private String resolveAccessorName(Method method) {

        confirmAsAccessor(method);

        String name = method.getName();

        if (name.startsWith("is")) {
            return lowerFirst(name.substring(2));
        }

        if (name.startsWith("get")) {
            return lowerFirst(name.substring(3));
        }

        if (name.startsWith("set")) {
            throw new IllegalArgumentException(name + "(): method name doesn't conform to accessor pattern (need isX or getX)");
        }

        logger.warn("Non-standard method name '{}' (better be isX or getX)", name);

        return name;
    }

    /**
     * Check if the method is an accessor method.
     *
     * @param method Method to check.
     *
     * @exception IllegalArgumentException if the method is not an accessor.
     */
    private void confirmAsAccessor(Method method) {

        ThreadContext.push("isAccessor(" + method.getName() + ")");

        try {

            if (method.getParameterTypes().length != 0) {
                throw new IllegalArgumentException(method.getName() + "() is not an accessor (takes arguments)");
            }

            logger.debug("returns {}", method.getReturnType().getName());

            if (method.getReturnType().equals(void.class) || method.getReturnType().equals(Void.class)) {
                throw new IllegalArgumentException(method.getName() + "() is not an accessor (returns void)");
            }

        } finally {
            ThreadContext.pop();
        }
    }

    /**
     * Convert the first character to lower case.
     *
     * No sanity checks beyond {@code null} or empty are done - if the string is all caps, the result will be exactly
     * what you'd think it is.
     *
     * @param source Source string.
     * @return The source string with the first character converted to lower case, or {@code null}
     * or empty string, if the source was {@code null} or empty.
     */
    public static String lowerFirst(String source) {

        if (source == null || source.isEmpty()) {
            return source;
        }

        if (source.length() == 1) {
            return source.toLowerCase();
        }

        return source.substring(0, 1).toLowerCase() + source.substring(1);
    }

    /**
     * Convert the first character to upper case.
     *
     * @param source Source string.
     * @return The source string with the first character converted to upper case, or {@code null}
     * or empty string, if the source was {@code null} or empty.
     */
    public static String upperFirst(String source) {

        if (source == null || source.isEmpty()) {
            return source;
        }

        if (source.length() == 1) {
            return source.toUpperCase();
        }

        return source.substring(0, 1).toUpperCase() + source.substring(1);
    }

    /**
     * JMX invocation proxy.
     *
     * Facilitates converting {@link #getAttribute(String)}, {@link #setAttribute(Attribute)},
     * {@link #getAttributeListString(String[])} and {@link #setAttributes(AttributeList)}
     * into actual method invocations of methods on the {@link #target}.
     */
    private class Proxy implements DynamicMBean {

        /**
         * Object to invoke methods on.
         */
        private final Object target;

        /**
         * {@link #target} class.
         */
        private final Class<?> targetClass;

        /**
         * Target JMX descriptor.
         */
        private final MBeanInfo mbInfo;

        /**
         * Mapping of the attribute name to the accessor method.
         */
        private final Map<String, Method> name2accessor = new TreeMap<>();

        /**
         * Mapping of the attribute name to the mutator method.
         */
        private final Map<String, Method> name2mutator = new TreeMap<>();

        private Proxy(Object target, MBeanInfo mbInfo, Map<Method, Method> accessor2mutator) {

            ThreadContext.push("Proxy()");

            try {

                if (target == null) {
                    throw new IllegalArgumentException(NULL_TARGET);
                }

                this.target = target;
                this.mbInfo = mbInfo;

                targetClass = target.getClass();

                MBeanAttributeInfo[] attributes = mbInfo.getAttributes();

                for (MBeanAttributeInfo attributeInstance : attributes) {

                    String attributeName = attributeInstance.getName();
                    logger.debug("Attribute: {}", attributeName);

                    Method accessor = resolve(attributeName, attributeInstance.isIs(), false);
                    name2accessor.put(attributeName, accessor);

                    Method mutator = accessor2mutator.get(accessor);

                    if (mutator != null) {
                        name2mutator.put(attributeName, mutator);
                    }
                }

                for (Entry<String, Method> entry : name2accessor.entrySet()) {

                    logger.debug("Accessor resolved for {}: {}", entry.getKey(), entry.getValue());
                }

                for (Entry<String, Method> entry : name2mutator.entrySet()) {

                    logger.debug("Mutator resolved for {}: {}", entry.getKey(), entry.getValue());
                }

            } finally {
                ThreadContext.pop();
            }
        }

        /**
         * Resolve the method instance by name.
         *
         * @param methodName Method name.
         * @param isIs Is this method a "isX" method.
         * @param recurse {@code true} if this is a recursive invocation.
         *
         * @return Method instance.
         *
         * @exception UnsupportedOperationException if a method instance can't be resolved.
         */
        private Method resolve(String methodName, boolean isIs, boolean recurse) {

            ThreadContext.push("resolve(" + methodName + ')');

            try {

                if (isIs) {
                    return resolve("is" + upperFirst(methodName), false, true);
                }

                try {

                    // Try straight (this is the last resort for the case of getX)

                    var targetMethod = targetClass.getMethod(methodName);

                    logger.debug("Resolved {}", targetMethod);

                    return targetMethod;

                } catch (NoSuchMethodException e) {

                    // If the name is isX or getX, we're screwed

                    if (recurse && (methodName.startsWith("is") || methodName.startsWith("get"))) {
                        throw new UnsupportedOperationException("Unable to find method '" + methodName + "', is* and get* tried too");
                    }

                    // Didn't work, try getX
                    logger.debug("Invocation failed: {}", e.getMessage());
                    return resolve("get" + upperFirst(methodName), false, true);
                }
            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        public Object getAttribute(String attribute) throws AttributeNotFoundException, ReflectionException {

            ThreadContext.push("getAttribute(" + attribute + ')');
            try {

                var method = name2accessor.get(attribute);

                if (method == null) {
                    throw new AttributeNotFoundException(attribute);
                }

                return method.invoke(target);

            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new ReflectionException(e, "Oops");
            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        public void setAttribute(Attribute attribute) throws AttributeNotFoundException {

            ThreadContext.push("setAttribute(" + attribute + ")");

            try {

                var method = name2mutator.get(attribute.getName());

                logger.debug("Mutator for {}: {}", attribute.getName(), method);

                if (method == null) {
                    throw new AttributeNotFoundException(attribute.getName());
                }

                try {

                    method.invoke(target, attribute.getValue());

                } catch (Throwable t) { // NOSONAR Consequences have been considered

                    // It's OK to log *and* rethrow, it'll only show up in jconsole
                    logger.error("Failed to setAttribute({})", attribute, t);

                    throw new IllegalStateException("Failed to setAttribute(" + attribute + ")", t);
                }
            } finally {
                ThreadContext.pop();
            }
        }

        @Override
        public AttributeList getAttributes(String[] attributes) {

            ThreadContext.push(getAttributeListString(attributes));
            try {

                var attributeList = new AttributeList();

                for (String attributeName : attributes) {
                    var method = name2accessor.get(attributeName);

                    if (method == null) {
                        throw new IllegalArgumentException("No accessor for '" + attributeName + "', available: " + name2accessor.keySet());
                    }

                    try {

                        Object value = method.invoke(target);
                        attributeList.add(new Attribute(attributeName, value));

                        logger.debug("{}: {}", attributeName, value);

                    } catch (IllegalAccessException | InvocationTargetException e) {
                        logger.error("invocation failed, attribute skipped: {}", attributeName, e);
                    }
                }

                return attributeList;

            } finally {
                ThreadContext.pop();
            }
        }

        private String getAttributeListString(String[] attributes) {

            StringBuilder sb = new StringBuilder("getAttributes(");

            for (int offset = 0; offset < attributes.length; offset++) {
                if (offset != 0) {
                    sb.append(", ");
                }
                sb.append(attributes[offset]);
            }
            sb.append(')');

            return sb.toString();
        }

        @Override
        public AttributeList setAttributes(AttributeList attributes) {

            ThreadContext.push("setAttributes(" + attributes + ")");

            try {

                for (Attribute a : attributes.asList()) {

                    try {

                        // VT: FIXME: Implement this one day, eh?

                    } catch (Throwable t) { // NOSONAR Consequences have been considered
                        logger.error("setAttribute({}) failed, moving on", a, t);
                    }
                }

                logger.error("Not Implemented", new UnsupportedOperationException(UNSUPPORTED));

            } finally {
                ThreadContext.pop();
            }

            throw new UnsupportedOperationException(UNSUPPORTED);
        }

        @Override
        public Object invoke(String actionName, Object[] params, String[] signature) {

            ThreadContext.push("invoke(" + actionName + ")");

            try {

                logger.error("Not Implemented", new UnsupportedOperationException(UNSUPPORTED));

            } finally {
                ThreadContext.pop();
            }

            throw new UnsupportedOperationException("Not Supported Yet: invoke(" + actionName + ")");
        }

        @Override
        public MBeanInfo getMBeanInfo() {
            return mbInfo;
        }
    }
}
