package net.sf.dz3.view.http.v2;

import java.util.Map;

import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.view.ConnectorFactory;

/**
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2011
 */
public class ThermostatFactory extends ConnectorFactory<JsonRenderer> {
    
    private final Scheduler scheduler;
    
    /**
     * Create an instance.
     * 
     * @param scheduler The scheduler. Can be {@code null}.
     */
    public ThermostatFactory(Scheduler scheduler) {
    
        this.scheduler = scheduler;
        
        if (scheduler != null) {
            
            // Lest there be a situation when someone adds more than one and forgets which one it is
            logger.info("Using scheduler: " + scheduler);
        }
    }

    @Override
    public JsonRenderer createComponent(Object source, Map<String, Object> context) {

        return new ThermostatRenderer((ThermostatModel) source, context, scheduler);
    }

    @Override
    public Class<?> getSourceClass() {

        return ThermostatModel.class;
    }
}
