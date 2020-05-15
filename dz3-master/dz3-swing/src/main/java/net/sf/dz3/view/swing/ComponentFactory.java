package net.sf.dz3.view.swing;

import java.util.Map;

import javax.swing.JComponent;

import net.sf.dz3.view.ConnectorFactory;

/**
 * Component factory base.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public abstract class ComponentFactory extends ConnectorFactory<JComponent> {

    /**
     * Create a component for the given source object,
     * and link it to the source object if necessary.
     *
     * @param source One of DZ objects.
     * @param context Necessary common objects as {@code key=value}.
     *
     * @return The component to display the source object.
     */
    @Override
    public abstract JComponent createComponent(
            Object source,
            Map<String, Object> context);
}
