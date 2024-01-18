package net.sf.dz3r.device.mqtt.v1;

import java.util.Objects;

/**
 * MQTT endpoint definition.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2021
 */
public class MqttEndpoint implements Comparable<MqttEndpoint> {

    public static final int DEFAULT_PORT = 1883;

    public final String host;
    public final int port;

    public MqttEndpoint(String host) {
        this(host, DEFAULT_PORT);
    }
    public MqttEndpoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    @Override
    public int compareTo(MqttEndpoint o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public boolean equals(Object other) {

        return other instanceof MqttEndpoint endpoint
                && host.equals(endpoint.host)
                && port == endpoint.port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
