package net.sf.dz3.view.swing.thermostat;

import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.view.swing.CellAndPanel;
import net.sf.dz3.view.swing.ComponentPairFactory;
import net.sf.dz3.view.swing.ScreenDescriptor;
import net.sf.dz3.view.swing.TemperatureUnit;
import org.apache.logging.log4j.ThreadContext;

import java.util.Map;

/**
 * Factory to create a panel to represent a {@link ThermostatModel}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2020
 */
public class ThermostatFactory extends ComponentPairFactory<ZoneCell, ThermostatPanel> {

    private final TemperatureUnit defaultUnit;

    public ThermostatFactory(TemperatureUnit defaultUnit) {
        this.defaultUnit = defaultUnit;
    }

    @Override
    public Class<?> getSourceClass() {
        return ThermostatModel.class;
    }

    /**
     * Create a panel representing a thermostat.
     * @return
     */
    @Override
    public CellAndPanel createComponent(Object source, Map<String, Object> context) {

        ThreadContext.push("createComponent");

        try {

            var cell = new ZoneCell((ThermostatModel) source);
            var panel = new ThermostatPanel(
                    (ThermostatModel) source,
                    (ScreenDescriptor) context.get("screen descriptor"),
                    (Scheduler) context.get("scheduler"),
                    defaultUnit);

            return new CellAndPanel(cell, panel);

        } finally {
            ThreadContext.pop();
        }
    }
}
