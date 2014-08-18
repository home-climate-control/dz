package net.sf.dz3.device.sensor.impl.onewire;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.utils.OWPath;

/**
 * 1-Wire&reg; network event. This is almost an exact clone of {@code
 * com.dalsemi.onewire.utils.NetworkMonitorEvent}, and the only reason for this
 * class to exist is that the event source is not a {@code
 * com.dalsemi.onewire.utils.NetworkMonitor}, but
 * {@link OneWireNetworkMonitor OneWireNetworkMonitor}.
 * 
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 2001-2009
 */
public class OwapiNetworkEvent extends OneWireNetworkEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Adapter that originated this event.
     */
    public final DSPortAdapter adapter;

    /**
     * 1-Wire path for this event.
     */
    public final OWPath path;

    /**
     * Create an instance.
     * 
     * @param monitor Monitor that is a source of this event.
     * @param adapter Adapter that is a source of this event.
     * @param address 1-Wire address of a device this event is associated with.
     * @param path 1-Wire path of the device.
     */
    public OwapiNetworkEvent(OneWireNetworkMonitor monitor, DSPortAdapter adapter, String address, OWPath path) {

        super(monitor, address);

        this.adapter = adapter;
        this.path = path;
    }

    /**
     * Get the 1-Wire device that this event is about as a 1-Wire container.
     * 
     * @return 1-Wire container for event 1-Wire device.
     */
    public OneWireContainer getDeviceContainer() {

        return adapter.getDeviceContainer(address);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        return "1-Wire NetEvent[" + address + " @ " + path + "]";
    }
}
