package net.sf.dz3.view.swing.sensor;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.swing.ComponentFactory;

import javax.swing.JComponent;
import java.util.Map;

public class SensorFactory extends ComponentFactory {

    @Override
    public Class<?> getSourceClass() {
        return AnalogSensor.class;
    }

    @Override
    public JComponent createComponent(Object source, Map<String, Object> context) {
        return new SensorPanel((AnalogSensor) source);
    }
}
