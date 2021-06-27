package net.sf.dz3.view.swing.sensor;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.swing.CellAndPanel;
import net.sf.dz3.view.swing.ComponentPairFactory;

import java.util.Map;

public class SensorFactory extends ComponentPairFactory {

    @Override
    public Class<?> getSourceClass() {
        return AnalogSensor.class;
    }

    @Override
    public CellAndPanel createComponent(Object source, Map<String, Object> context) {
        return new CellAndPanel(new SensorCell(), new SensorPanel((AnalogSensor) source));
    }
}
