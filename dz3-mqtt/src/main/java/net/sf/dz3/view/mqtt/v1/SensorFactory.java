package net.sf.dz3.view.mqtt.v1;

import java.util.Map;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.ConnectorFactory;

/**
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2019
 */
public class SensorFactory extends ConnectorFactory<JsonRenderer> {

    @Override
    public JsonRenderer createComponent(Object source, Map<String, Object> context) {

        return new SensorRenderer((AnalogSensor) source, context);
    }

    @Override
    public Class<?> getSourceClass() {

        return AnalogSensor.class;
    }
}
