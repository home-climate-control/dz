package net.sf.dz3r.device.mqtt.v1;

import net.sf.dz3r.device.mqtt.MqttListener;

/**
 * MQTT stream cold publisher.
 *
 * Doesn't implement the {@link net.sf.dz3r.signal.SignalSource} interface - no need at this point
 * in the data pipeline, DZ entities haven't been resolved yet.
 *
 * If you need to publish messages, use {@link MqttAdapterImpl} instead.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class MqttListenerImpl extends AbstractMqttAdapter implements MqttListener {

    /**
     * Create an unauthenticated instance that will NOT automatically reconnect.
     *
     * @param address MQTT broker endpoint.
     */
    public MqttListenerImpl(MqttEndpoint address) {
        this(address, null, null, false);
    }

    public MqttListenerImpl(MqttEndpoint address, String username, String password, boolean autoReconnect) {
        super(address, username, password, autoReconnect);
    }
}
