package net.sf.dz3.controller;

/**
 * {@link HysteresisController} status.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001-2009
 */
public class HysteresisControllerStatus extends ProcessControllerStatus {

    /**
     * Controller state.
     */
    public final boolean state;

    public HysteresisControllerStatus(ProcessControllerStatus superStatus, boolean state) {
        super(superStatus.setpoint, superStatus.error, superStatus.signal);

        this.state = state;
    }

    /**
     * Get a string representation of a hysteresis controller status.
     *
     * @return A string representation of a status put together by a
     * superclass, followed by the {@link #state current state}.
     */
    @Override
    public final String toString() {

        return super.toString() + "@" + state;
    }
}
