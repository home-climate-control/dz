package net.sf.dz3.view.http.v1;

import java.util.Map;

import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.ConnectorFactory;

/**
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */
public class SensorFactory extends ConnectorFactory<RestRenderer> {

    @Override
    public RestRenderer createComponent(Object source, Map<String, Object> context) {

        return new SensorRenderer((AnalogSensor) source, context);
    }

    @Override
    public Class<?> getSourceClass() {

        return AnalogSensor.class;
    }
}
