package net.sf.dz3.view.swing;

import net.sf.dz3.view.ConnectorFactory;

import java.util.Map;

/**
 * Component pair factory base.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public abstract class ComponentPairFactory<C extends EntityCell, P extends EntityPanel> extends ConnectorFactory<CellAndPanel> {

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
    public abstract CellAndPanel createComponent(
            Object source,
            Map<String, Object> context);
}
