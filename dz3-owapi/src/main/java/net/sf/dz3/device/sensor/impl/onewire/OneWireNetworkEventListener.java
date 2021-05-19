package net.sf.dz3.device.sensor.impl.onewire;

/**
 * {@code OneWireNetworkMonitor} listener.
 * 
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2009
 */
public interface OneWireNetworkEventListener {

    /**
     * Handle a network arrival.
     * 
     * @param e Event object containing the information about the network
     * arrival.
     */
    public void networkArrival(OneWireNetworkEvent e);

    /**
     * Handle a network departure.
     * 
     * @param e Event object containing the information about the network
     * departure.
     */
    public void networkDeparture(OneWireNetworkEvent e);

    /**
     * Handle a network fault.
     * 
     * @param e Event object containing the information about the network
     * fault.
     * @param message Fault message.
     */
    public void networkFault(OneWireNetworkEvent e, String message);
}
