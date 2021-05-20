package net.sf.dz3.view.swing.thermostat;

import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.view.swing.ScreenDescriptor;
import net.sf.dz3.view.swing.TemperatureUnit;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.event.KeyEvent;

import static org.mockito.Mockito.mock;

class ThermostatPanelTest {

    @Test
    void nullScheduler() {

        ThermostatModel tm = new ThermostatModel("model", mock(AnalogSensor.class), new SimplePidController(1, 1, 1, 1, 1));
        ThermostatPanel tp = new ThermostatPanel(tm, new ScreenDescriptor("", new Dimension(0, 0), null, null, null), null, TemperatureUnit.CELSIUS);

        tp.keyPressed(new KeyEvent(tp, 0, 0, 0, 0, 's', 0));
    }
}
