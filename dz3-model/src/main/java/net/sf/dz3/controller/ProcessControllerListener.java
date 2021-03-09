package net.sf.dz3.controller;

/**
 * Process controller listener.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org"> Vadim Tkachenko</a> 2001
 * @version $Id: ProcessControllerListener.java,v 1.2 2004-06-28 20:35:46 vtt Exp $
 */
public interface ProcessControllerListener {

    /**
     * Accept notification about the process controller status change.
     *
     * @param source The process controller whose status has changed.
     *
     * @param status Process controller data. This object is generic because
     * there are many different kinds of process controller which expose
     * different features. Preferrably, this is the same object that is
     * exposed by {@link ProcessController#getStatus()
     * ProcessController.getStatus()} method.
     */
    void processControllerStatusChanged(ProcessController source, ProcessControllerStatus status);

    /**
     * Accept the notification about the controller setpoint change.
     *
     * @param source The process controller whose setpoint has changed.
     *
     * @param setpoint Current value of the process controller setpoint.
     */
    void setpointChanged(ProcessController source, double setpoint);
}
