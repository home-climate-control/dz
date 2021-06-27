package net.sf.dz3.view.swing.unit;

import net.sf.dz3.device.model.RuntimePredictor;
import net.sf.dz3.view.swing.CellAndPanel;
import net.sf.dz3.view.swing.ComponentPairFactory;

import java.util.Map;

public class UnitFactory extends ComponentPairFactory {

    @Override
    public Class<?> getSourceClass() {
        return RuntimePredictor.class;
    }

    @Override
    public CellAndPanel createComponent(Object source, Map<String, Object> context) {
        return new CellAndPanel(new UnitCell((RuntimePredictor) source), new UnitPanel((RuntimePredictor) source));
    }
}
