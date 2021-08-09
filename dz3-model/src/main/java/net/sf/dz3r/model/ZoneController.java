package net.sf.dz3r.model;

import net.sf.dz3r.controller.ProcessController;
import net.sf.dz3r.signal.Signal;
import net.sf.dz3r.signal.ZoneStatus;
import reactor.core.publisher.Flux;

/**
 * Accepts signals from {@link Zone zones} and issues signals to Unit and Damper Controller.
 *
 * VT: FIXME: Augment the description with links once those entities are ported to reactive streams.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class ZoneController implements ProcessController<ZoneStatus, Double> {

    private static final String MAKES_NO_SENSE = "This operation makes no sense for ZoneController";

    @Override
    public void setSetpoint(double setpoint) {
        throw new UnsupportedOperationException(MAKES_NO_SENSE);
    }

    @Override
    public double getSetpoint() {
        throw new UnsupportedOperationException(MAKES_NO_SENSE);
    }

    @Override
    public Signal<ZoneStatus> getProcessVariable() {
        throw new UnsupportedOperationException(MAKES_NO_SENSE);
    }

    @Override
    public double getError() {
        throw new UnsupportedOperationException(MAKES_NO_SENSE);
    }

    @Override
    public Flux<Signal<Status<Double>>> compute(Flux<Signal<ZoneStatus>> pv) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
