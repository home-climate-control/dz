package net.sf.dz3r.device.mqtt.v2;

import net.sf.dz3r.common.HCCObjects;
import net.sf.dz3r.device.mqtt.MqttListener;
import net.sf.dz3r.device.mqtt.v1.MqttEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

/**
 * Base class for {@link net.sf.dz3r.device.mqtt.v2async.MqttListenerImpl}}.
 *
 * @author Copyright &copy; <a href="mailto:vt@homeclimatecontrol.com">Vadim Tkachenko</a> 2001-2023
 */
abstract public class AbstractMqttListener implements MqttListener {
    public static final Duration DEFAULT_CACHE_AGE = Duration.ofSeconds(30);

    protected final Logger logger = LogManager.getLogger();

    public final MqttEndpoint address;
    protected final String username;
    protected final String password;
    public final boolean autoReconnect;
    public final Duration cacheFor;

    private boolean closed = false;

    protected AbstractMqttListener(MqttEndpoint address, String username, String password, boolean autoReconnect, Duration cacheFor) {

        this.address = HCCObjects.requireNonNull(address, "address can't be null");
        this.username = username;
        this.password = password;
        this.autoReconnect = autoReconnect;
        this.cacheFor = HCCObjects.requireNonNull(cacheFor, "cacheFor can't be null");
    }
    protected final void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Already close()d");
        }
    }

    /**
     * Close and mark unusable.
     */
    @Override
    public void close() throws Exception {
        closed = true;
    }

    @Override
    public final MqttEndpoint getAddress() {
        return address;
    }

    public record ConnectionKey(
            String topic,
            boolean includeSubtopics
    ) {

    }
}
