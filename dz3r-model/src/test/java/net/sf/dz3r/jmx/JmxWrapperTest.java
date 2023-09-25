package net.sf.dz3r.jmx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Test;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
class JmxWrapperTest {

    private final Logger logger = LogManager.getLogger(getClass());
    private final Random rg = new Random();

    private ObjectName getObjectName() throws MalformedObjectNameException {
        var properties = new Hashtable<String, String>();

        properties.put("id", Double.toString(rg.nextGaussian()));
        return new ObjectName("testDomain", properties);
    }

    @Test
    void testExposeNull() throws Throwable {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JmxWrapper().expose(null, getObjectName(), "null"))
                .withMessage("target can't be null");
    }

    @Test
    void testLiteral() {
        assertThatCode(() -> new JmxWrapper().expose(new LiteralAccessor(), getObjectName(), "Literal accessor"))
                .doesNotThrowAnyException();
    }

    @Test
    void testGoodAccessor() {
        assertThatCode(() -> new JmxWrapper().expose(new GoodAccessor(), getObjectName(), "Properly named get* accessor"))
                .doesNotThrowAnyException();
    }

    @Test
    void testIsAccessor() {
        assertThatCode(() -> new JmxWrapper().expose(new IsAccessor(), getObjectName(), "Properly named is* accessor"))
                .doesNotThrowAnyException();
    }

    @Test
    void testAccessorMutator() {
        assertThatCode(() -> new JmxWrapper().expose(new AccessorMutator(), getObjectName(), "Accessor & mutator"))
                .doesNotThrowAnyException();
    }

    @Test
    void testAccessorBadMutator() {
        assertThatCode(() -> new JmxWrapper().expose(new AccessorBadMutator(), getObjectName(), "Good accessor, bad mutator"))
                .doesNotThrowAnyException();
    }

    @Test
    void testBadAccessorHasArguments() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JmxWrapper().expose(new BadAccessorHasArguments(), getObjectName(), "Bad accessor signature - takes arguments"))
                .withMessage("name() is not an accessor (takes arguments)");
    }

    @Test
    void testBadAccessorReturns_void() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JmxWrapper().expose(new BadAccessorReturns_void(), getObjectName(), "Bad accessor signature - returns void"))
                .withMessage("name() is not an accessor (returns void)");
    }

    @Test
    void testBadAccessorReturnsVoid() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JmxWrapper().expose(new BadAccessorReturnsVoid(), getObjectName(), "Bad accessor signature - returns void"))
                .withMessage("name() is not an accessor (returns void)");
    }

    @Test
    void testBadAccessorWrongName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JmxWrapper().expose(new BadAccessorWrongName(), getObjectName(), "Bad accessor signature - returns void"))
                .withMessage("setName(): method name doesn't conform to accessor pattern (need isX or getX)");
    }

    @Test
    void testInterfaceDefined() {
        assertThatCode(() -> new JmxWrapper().expose(new TheImplementation(), getObjectName(), "Annotation on the interface"))
                .doesNotThrowAnyException();
    }

    @Test
    void testCollectionConstructor() {

        var targets = new HashSet<>();

        targets.add(new LiteralAccessor());
        targets.add(new GoodAccessor());
        targets.add(new AccessorMutator());
        targets.add(new AccessorBadMutator());
        targets.add(new BadAccessorHasArguments());
        targets.add(new BadAccessorReturnsVoid());
        targets.add(new TheImplementation());
        targets.add(new TheConcreteSuperclass());
        targets.add("something that is definitely not @JmxAware");
        targets.add(new SimpleJmxAware());

        assertThatCode(() -> new JmxWrapper(targets)).doesNotThrowAnyException();
    }

    @Test
    void testRegisterNull() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JmxWrapper().register(null))
                .withMessage("target can't be null");
    }

    @Test
    void testRegisterNotJmxAware() {
        assertThatCode(() -> new JmxWrapper().register("something that is definitely not @JmxAware"))
                .doesNotThrowAnyException();
    }

    @Test
    void testRegisterJmxAware() {
        assertThatCode(() -> new JmxWrapper().register(new SimpleJmxAware())).doesNotThrowAnyException();
    }

    @Test
    void testRegisterTwice() {
        assertThatCode(() -> {
            JmxWrapper w = new JmxWrapper();
            Object target = new SimpleJmxAware();

            w.register(target);
            w.register(target);

        }).doesNotThrowAnyException();
    }

    @Test
    void testLowerFirst() {
        assertThat(JmxWrapper.lowerFirst(null)).isNull();
        assertThat(JmxWrapper.lowerFirst("")).isEmpty();
        assertThat(JmxWrapper.lowerFirst("A")).isEqualTo("a");
        assertThat(JmxWrapper.lowerFirst("1")).isEqualTo("1");
        assertThat(JmxWrapper.lowerFirst("TEST")).isEqualTo("tEST");
    }

    @Test
    void testUpperFirst() {
        assertThat(JmxWrapper.upperFirst(null)).isNull();
        assertThat(JmxWrapper.upperFirst("")).isEmpty();
        assertThat(JmxWrapper.upperFirst("a")).isEqualTo("A");
        assertThat(JmxWrapper.upperFirst("1")).isEqualTo("1");
        assertThat(JmxWrapper.upperFirst("test")).isEqualTo("Test");
    }

    @Test
    void testAttributes() throws Throwable {

        ThreadContext.push("testAttributes");

        try {

            var mbs = ManagementFactory.getPlatformMBeanServer();
            var target = new SimpleJmxAware();
            var jmxWrapper = new JmxWrapper();

            jmxWrapper.register(target);

            JmxDescriptor jmxDescriptor = target.getJmxDescriptor();

            var pattern = jmxDescriptor.domainName + ":" +
                    "name=" + jmxDescriptor.name + "," +
                    "instance=" + jmxDescriptor.instance;
            logger.info("getAttribute name: {}", pattern);

            var objectName = new ObjectName(pattern);

            var found = mbs.queryMBeans(objectName, null);

            logger.info("found: {}", found);

            assertThat(found).hasSize(1);
            assertThat(found.iterator().next().getObjectName()).isEqualTo(objectName);

            {
                // The good behavior case

                var code = mbs.getAttribute(objectName, "code");

                logger.info("getAttribute(code): {}", code);

                assertThat(target.getCode()).isEqualTo(code);

                mbs.setAttribute(objectName, new Attribute("code", "DUDE"));

                logger.info("setAttribute(code): {}", target.getCode());

                assertThat(target.getCode()).isEqualTo("DUDE");
            }

            {
                // The bad behavior case

                assertThatExceptionOfType(ReflectionException.class)
                        .isThrownBy(() -> {

                            // An unexpected exception in an accessor
                            mbs.getAttribute(objectName, "error");

                        }).withCause(new InvocationTargetException(new NullPointerException("Nobody expects the Spanish Inquisition!")));

                var attribute = new Attribute("error", "DUDE");
                assertThatExceptionOfType(RuntimeMBeanException.class)
                        .isThrownBy(() -> {

                            // An unexpected exception in a mutator
                            mbs.setAttribute(objectName, attribute);

                        })
                        .withMessage("java.lang.IllegalStateException: Failed to setAttribute(error = DUDE)")
                        .withCause(
                                new IllegalStateException(
                                        "Failed to setAttribute(error = DUDE)",
                                        new InvocationTargetException(
                                                new NullPointerException("NOBODY expects the Spanish Inquisition!"))));

                assertThatExceptionOfType(AttributeNotFoundException.class)
                        .isThrownBy(() -> {

                            // Nonexistent accessor
                            mbs.getAttribute(objectName, "nonexistent");

                        }).withMessage("nonexistent");

                assertThatExceptionOfType(AttributeNotFoundException.class)
                        .isThrownBy(() -> {

                            // Nonexistent mutator
                            mbs.setAttribute(objectName, new Attribute("nonexistent", "DUDE"));

                        }).withMessage("nonexistent");

                assertThatExceptionOfType(AttributeNotFoundException.class)
                        .isThrownBy(() -> {

                            // Inaccessible accessor
                            mbs.getAttribute(objectName, "secret");

                        }).withMessage("secret");

                assertThatExceptionOfType(AttributeNotFoundException.class)
                        .isThrownBy(() -> {

                            // Inaccessible mutator
                            mbs.setAttribute(objectName, new Attribute("secret", "DUDE"));

                        }).withMessage("secret");
            }

            assertThatExceptionOfType(RuntimeMBeanException.class)
                    .isThrownBy(() -> mbs.invoke(objectName, "hashCode", null, null))
                    .withCause(new UnsupportedOperationException("Not Supported Yet: invoke(hashCode)"));

        } finally {
            ThreadContext.pop();
        }
    }

    static class LiteralAccessor {

        @JmxAttribute(description="just the name")
        public String name() {
            return "name";
        }
    }

    static class GoodAccessor {

        @JmxAttribute(description="just the name")
        public String getName() {
            return "name";
        }
    }

    static class IsAccessor {

        @JmxAttribute(description="is enabled?")
        public boolean isEnabled() {
            return true;
        }
    }

    static class AccessorMutator {

        @JmxAttribute(description="just the name")
        public String getName() {
            return "name";
        }

        public void setName(String name) {
        }
    }

    static class AccessorBadMutator {

        @JmxAttribute(description="just the name")
        public String getName() {
            return "name";
        }

        public void setName(Set<?> name) {
        }
    }

    static class BadAccessorHasArguments {

        @JmxAttribute(description="just the name")
        //@ConfigurableProperty(
        //  propertyName="name",
        //  description="name given"
        //)
        public String name(String key) {
            return "name";
        }
    }

    static class BadAccessorReturns_void {

        @JmxAttribute(description="just the name")
        public void name() {
        }
    }

    static class BadAccessorReturnsVoid {

        @JmxAttribute(description="just the name")
        public Void name() {
            return null;
        }
    }

    static class BadAccessorWrongName {

        @JmxAttribute(description="just the name")
        public String setName() {
            return "name";
        }
    }

    interface TheInterface {

        @JmxAttribute(description="defined in the interface")
        String getInterfaceDefined();
    }

    class TheConcreteSuperclass {

        @JmxAttribute(description = "defined in the concrete superclass")
        public String getConcreteSuperclassDefined() {
            return "concrete superclass";
        }
    }

    abstract class TheAbstractSuperclass extends TheConcreteSuperclass {

        @JmxAttribute(description = "defined in the abstract superclass")
        public abstract String getAbstractSuperclassDefined();
    }

    class TheImplementation extends TheAbstractSuperclass implements TheInterface {

        @Override
        public String getInterfaceDefined() {
            return "must be exposed though the annotation is present only on the interface";
        }

        @Override
        public String getAbstractSuperclassDefined() {
            return "must be exposed through the annotation is present only on the abstract superclass";
        }

        @Override
        public String getConcreteSuperclassDefined() {
            return "must be exposed through the annotation is present only on the concrete superclass";
        }
    }

    class SimpleJmxAware implements JmxAware {

        private String code = Integer.toHexString(rg.nextInt());

        @Override
        public JmxDescriptor getJmxDescriptor() {
            return new JmxDescriptor("jukebox", getClass().getSimpleName(), Integer.toHexString(hashCode()), "test case");
        }

        @JmxAttribute(description = "random code")
        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        @JmxAttribute(description = "broken method")
        public String getError() {
            throw new NullPointerException("Nobody expects the Spanish Inquisition!");
        }

        public void setError(String error) {
            throw new NullPointerException("NOBODY expects the Spanish Inquisition!");
        }

        @JmxAttribute(description = "inaccessible method")
        private String getSecret() {
            return "secret";
        }

        private void setSecret(String secret) {
        }
    }

    @Test
    void testNullJmxDescriptor() {

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JmxWrapper().register(new NullJmxDescriptor())).withMessage("JMX Descriptor can't be null");
    }

    class NullJmxDescriptor implements JmxAware {

        @Override
        public JmxDescriptor getJmxDescriptor() {
            return null;
        }
    }

    @Test
    void testIspanAndGetAttributes() throws MalformedObjectNameException, ReflectionException, InstanceNotFoundException {

        var mBeanServer = ManagementFactory.getPlatformMBeanServer();
        var beanCount = mBeanServer.getMBeanCount();

        assertThatCode(() -> new JmxWrapper().register(new FunkyName())).doesNotThrowAnyException();
        assertThat(mBeanServer.getMBeanCount()).isEqualTo(beanCount + 1);

        // Testing getAttributes separately may create a race condition unless a different name is used,
        // so let's just test it here

        var name = new ObjectName("jukebox:name=span,instance=instance");
        var attributes = mBeanServer.getAttributes(name, new String[] { "ispan"});

        assertThat(attributes).hasSize(1);

        logger.info("attributes[0]: {}", attributes.get(0));
    }

    class FunkyName implements JmxAware {

        @JmxAttribute(description = "get Ispan")
        public long getIspan() {
            return 0;
        }

        public void setIspan(long Ispan) {
            // Do nothing
        }

        @Override
        public JmxDescriptor getJmxDescriptor() {
            return new JmxDescriptor("jukebox", "span", "instance", "description");
        }
    }
}
