package net.sf.dz3.device.sensor.impl.onewire;

import java.util.EventObject;

/**
 * 1-Wire&reg; network event. This is almost an exact clone of {@code
 * com.dalsemi.onewire.utils.NetworkMonitorEvent}, and the only reason for this
 * class to exist is that the event source is not a {@code
 * com.dalsemi.onewire.utils.NetworkMonitor}, but
 * {@code net.sf.dz.daemon.onewire.owapi.OneWireNetworkMonitor}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2009
 */
public class OneWireNetworkEvent extends EventObject {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Address of a device the event is associated with.
     */
    public final String address;

    /**
     * Create an instance.
     * 
     * @param source Event source.
     * @param address Address of a device the event is associated with.
     */
    public OneWireNetworkEvent(Object source, String address) {

        super(source);

        this.address = address;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        return "1-Wire NetEvent[" + address + "]";
    }
}
