package net.sf.dz3.view.mqtt.v1;

/**
 * Block of information to send to MQTT broker.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2019
 */
public class UpstreamBlock {

    /**
     * Topic relative to the root topic.
     */
    public final String topic;

    /**
     * JSON payload representing the object state.
     */
    public final String payload;

    /**
     * Create an instance.
     * 
     * @param topic Topic relative to the root topic.
     * @param payload JSON payload representing the object state.
     */
    public UpstreamBlock(String topic, String payload) {
        
        this.topic = topic;
        this.payload = payload;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("topic=").append(topic).append(", ");
        sb.append(payload);

        return sb.toString();
    }
}
