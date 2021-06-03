package net.sf.dz3.view.swing.thermostat;

import net.sf.dz3.controller.pid.SimplePidController;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.device.sensor.AnalogSensor;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.view.swing.ScreenDescriptor;
import net.sf.dz3.view.swing.TemperatureUnit;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.event.KeyEvent;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ThermostatPanelTest {

    /**
     * Make sure the panel doesn't break when there's no scheduler attached
     */
    @Test
    void nullScheduler() {

        assertThatCode(() -> {

            var tm = new ThermostatModel("model", mock(AnalogSensor.class), new SimplePidController(1, 1, 1, 1, 1));
            var tp = new ThermostatPanel(tm, new ScreenDescriptor("", new Dimension(0, 0), null, null, null), null, TemperatureUnit.CELSIUS);

            tp.keyPressed(new KeyEvent(tp, 0, 0, 0, 0, 's', 0));

        }).doesNotThrowAnyException();
    }

    /**
     * Make sure the panel doesn't break when there's no current period
     */
    @Test
    void nullPeriod() {

        assertThatCode(() -> {

            var tm = new ThermostatModel("model", mock(AnalogSensor.class), new SimplePidController(1, 1, 1, 1, 1));
            var scheduler = mock(Scheduler.class);
            var tp = new ThermostatPanel(tm, new ScreenDescriptor("", new Dimension(0, 0), null, null, null), scheduler, TemperatureUnit.CELSIUS);

            doReturn(null).when(scheduler).getCurrentPeriod(tm);
            tp.keyPressed(new KeyEvent(tp, 0, 0, 0, 0, 's', 0));

        }).doesNotThrowAnyException();
    }
}
