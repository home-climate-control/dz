package net.sf.dz3.view.swing.unit;

import net.sf.dz3.device.model.RuntimePredictor;
import net.sf.dz3.view.swing.ComponentFactory;

import javax.swing.JComponent;
import java.util.Map;

public class UnitFactory extends ComponentFactory {

    @Override
    public Class<?> getSourceClass() {
        return RuntimePredictor.class;
    }

    @Override
    public JComponent createComponent(Object source, Map<String, Object> context) {
        return new UnitPanel((RuntimePredictor) source);
    }
}
