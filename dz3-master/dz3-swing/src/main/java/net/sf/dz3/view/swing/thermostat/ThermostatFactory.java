package net.sf.dz3.view.swing.thermostat;

import java.util.Map;

import javax.swing.JComponent;

import org.apache.log4j.NDC;

import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.view.swing.ComponentFactory;
import net.sf.dz3.view.swing.ScreenDescriptor;

/**
 * Factory to create a panel to represent a {@link ThermostatModel}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2010
 */
public class ThermostatFactory extends ComponentFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getSourceClass() {
        
        return ThermostatModel.class;
    }

    /**
     * Create a panel representing a thermostat.
     */
    @Override
    public JComponent createComponent(Object source, Map<String, Object> context) {
        
        NDC.push("createComponent");
        
        try {
        
            return new ThermostatPanel((ThermostatModel) source, (ScreenDescriptor) context.get("screen descriptor"), (Scheduler) context.get("scheduler"));
            
        } finally {
            NDC.pop();
        }
    }
}
