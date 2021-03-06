package net.sf.dz3.device.sensor.impl;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import net.sf.dz3.device.sensor.Switch;
import net.sf.jukebox.datastream.logger.impl.DataBroadcaster;
import net.sf.jukebox.datastream.signal.model.DataSample;
import net.sf.jukebox.datastream.signal.model.DataSink;

/**
 * An abstract switch.
 *
 * Supports common configuration and listener notification features.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2019
 */
abstract public class AbstractSwitch implements Switch {

    protected final Logger logger = LogManager.getLogger(getClass());
    private final DataBroadcaster<Boolean> dataBroadcaster = new DataBroadcaster<Boolean>();

    /**
     * Switch address.
     */
    private final String address;

    /**
     * Switch state.
     */
    private boolean state = false;

    public AbstractSwitch(String address, boolean state) {

        this.address = address;
        this.state = state;
    }

    @Override
    public final boolean getState() throws IOException {

        return state;
    }

    @Override
    public synchronized final void setState(boolean state) throws IOException {

        ThreadContext.push("setState");

        try {

            logger.debug("Switch " + address + "=" + state);
            this.state = state;
            dataBroadcaster.broadcast(new DataSample<Boolean>(System.currentTimeMillis(), getAddress(), getAddress(), state, null));

        } finally {
            ThreadContext.pop();
        }
    }

    @Override
    public final String getAddress() {

        return address;
    }

    @Override
    public final void addConsumer(DataSink<Boolean> consumer) {
        dataBroadcaster.addConsumer(consumer);
    }

    @Override
    public final void removeConsumer(DataSink<Boolean> consumer) {
        dataBroadcaster.removeConsumer(consumer);
    }

    @Override
    public String toString() {

        String state;

        try {
            state = Boolean.toString(getState());
        } catch (IOException ex) {
            logger.error("exception ignored in toString()", ex);
            state = "error";
        }

        return "{" + getClass().getName() + ", address=" + getAddress() + ", state=" + state + "}";
    }
}
