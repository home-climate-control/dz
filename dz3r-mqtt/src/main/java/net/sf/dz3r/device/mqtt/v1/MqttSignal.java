package net.sf.dz3r.device.mqtt.v1;

/**
 * MQTT signal.
 * <p>
 * Doesn't implement the {@link net.sf.dz3r.signal.Signal} interface - no need at this point,
 * DZ entities haven't been resolved yet.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
public record MqttSignal(
        String topic,
        String message) {

}
