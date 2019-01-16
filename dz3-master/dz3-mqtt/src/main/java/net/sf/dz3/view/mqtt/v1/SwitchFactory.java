package net.sf.dz3.view.mqtt.v1;

import java.util.Map;

import net.sf.dz3.device.sensor.Switch;
import net.sf.dz3.view.ConnectorFactory;

/**
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2019
 */
public class SwitchFactory extends ConnectorFactory<JsonRenderer> {

    @Override
    public JsonRenderer createComponent(Object source, Map<String, Object> context) {

        return new SwitchRenderer((Switch) source, context);
    }

    @Override
    public Class<?> getSourceClass() {

        return Switch.class;
    }
}
