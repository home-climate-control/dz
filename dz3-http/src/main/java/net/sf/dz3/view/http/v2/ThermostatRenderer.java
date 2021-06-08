package net.sf.dz3.view.http.v2;

import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSample;
import com.homeclimatecontrol.jukebox.datastream.signal.model.DataSink;
import net.sf.dz3.controller.pid.AbstractPidController;
import net.sf.dz3.device.model.HvacMode;
import net.sf.dz3.device.model.ThermostatSignal;
import net.sf.dz3.device.model.ZoneState;
import net.sf.dz3.device.model.impl.ThermostatModel;
import net.sf.dz3.scheduler.Scheduler;
import net.sf.dz3.scheduler.Scheduler.Deviation;
import net.sf.dz3.view.http.common.QueueFeeder;
import org.apache.logging.log4j.LogManager;
import org.joda.time.DateTime;

import java.util.Map;

/**
/**
 * Keeps {@link #consume(DataSample) receiving} data notifications and
 * {@link #emit(ZoneSnapshot) stuffing} them into the queue.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */

public class ThermostatRenderer extends QueueFeeder<ZoneSnapshot> implements DataSink<ThermostatSignal>, JsonRenderer {

    private final ThermostatModel source;
    private final Scheduler scheduler;

    public ThermostatRenderer(ThermostatModel source, Map<String, Object> context, Scheduler scheduler) {

        super(context);

        this.source = source;
        this.scheduler = scheduler;

        // VT: NOTE: This is a simplification sufficient until the server side framework
        // is ironed out. Later, the controller signal needs to be added, just like it is
        // done in ThermostatPanel.

        source.addConsumer(this);
    }

    @Override
    public void consume(DataSample<ThermostatSignal> signal) {

        var name = source.getName();
        var hvacMode = ((AbstractPidController) source.getController()).getP() > 0 ? HvacMode.COOLING : HvacMode.HEATING;
        var state = renderState(signal);
        var thermostatSignal = signal.sample.demand.sample;

        double currentTemperature;

        var sample = source.getSensor().getSignal();

        // It would be a good idea to use Double.NaN for null and error cases. However,
        // this would later violate JSON specification, so let's leave this at 0 and see how it works.

        if (sample == null) {

            LogManager.getLogger().trace("current=null");
            currentTemperature = 0;

        } else if ( sample.isError() ) {

            LogManager.getLogger().trace("current=error");
            currentTemperature = 0;

        } else {

            currentTemperature = sample.sample;
        }

        var setpointTemperature = source.getController().getSetpoint();
        var enabled = signal.sample.enabled;
        var onHold = signal.sample.onHold;
        var voting = signal.sample.voting;
        var error = signal.isError() ? signal.error.getMessage() : null;

        var periodName = getPeriod();

        var deviation = scheduler == null ? new Deviation(0, false, false) : scheduler.getDeviation(source, setpointTemperature, enabled, voting, new DateTime(signal.timestamp));

        emit(new ZoneSnapshot(signal.timestamp, name, hvacMode, state, thermostatSignal, currentTemperature,
                setpointTemperature, enabled, onHold, voting, periodName,
                deviation.setpoint, deviation.enabled, deviation.voting,
                error));
    }

    private ZoneState renderState(DataSample<ThermostatSignal> signal) {

        if (signal.isError()) {
            return ZoneState.ERROR;
        }

        if (!signal.sample.enabled) {
            return ZoneState.OFF;
        }

        return signal.sample.calling ? ZoneState.CALLING : ZoneState.HAPPY;
    }

    /**
     * Find out the name of the current period for the zone served by this renderer.
     *
     * @return {@code null} if there's no scheduler, a predefined string if there's no period, or a period name.
     */
    private String getPeriod() {

        if (scheduler == null) {
            return null;
        }

        var p = scheduler.getCurrentPeriod(source);

        if (p == null) {
            return "(no period is active)";
        }

        return p.name;
    }

    @Override
    public String render(Object source) {
        return source.toString();
    }
}
