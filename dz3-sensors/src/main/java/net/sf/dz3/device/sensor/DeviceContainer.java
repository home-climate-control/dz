package net.sf.dz3.device.sensor;

/**
 * Implementation independent device container abstraction.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2000-2021
 */
public interface DeviceContainer extends Addressable {

    /**
     * @return Device name.
     */
    String getName();

    /**
     * @return Device address.
     */
    @Override
    String getAddress();

    /**
     * @return Device type.
     */
    SensorType getType();

    /**
     * @return Device signature.
     */
    String getSignature();
}
