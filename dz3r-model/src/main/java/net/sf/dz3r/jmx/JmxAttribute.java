package net.sf.dz3r.jmx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used to describe a property that needs to be exposed via JMX.
 *
 * Advertised name of the property is determined following JavaBeans model. Unlike classical JMX, the implementation
 * must support inheritance.
 *
 * For simplicity, this attribute is allowed only on an accessor ({@code get*()} or {@code is*()}) method.
 * However, if there is a mutator ({@code set*()}) method of a matching type, a proper JMX support should be provided.
 *
 * @author <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2004-2009
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JmxAttribute {

    /**
     * Human readable description.
     *
     * The actual JMX attribute value will be {@code ${name}.description}, where {@code name} is determined by
     * the framework taking care of exposing the actual value.
     *
     * @return Human readable description.
     */
    String description();
}
