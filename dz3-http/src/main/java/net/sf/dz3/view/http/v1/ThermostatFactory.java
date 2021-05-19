package net.sf.dz3.view.http.v1;

import java.util.Map;

import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.view.ConnectorFactory;

/**
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2010
 */
public class ThermostatFactory extends ConnectorFactory<RestRenderer> {

    @Override
    public RestRenderer createComponent(Object source, Map<String, Object> context) {

        return new ThermostatRenderer((ThermostatModel) source, context);
    }

    @Override
    public Class<?> getSourceClass() {

        return ThermostatModel.class;
    }
}
